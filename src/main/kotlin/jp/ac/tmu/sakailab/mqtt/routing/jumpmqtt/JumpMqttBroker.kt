package jp.ac.tmu.sakailab.mqtt.routing.jumpmqtt

import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.MsgTracer
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.routing.AMqttBroker
import jp.ac.tmu.sakailab.mqtt.routing.EncapMsg
import jp.ac.tmu.sakailab.mqtt.routing.PubMsg
import jp.ac.tmu.sakailab.mqtt.routing.PublicBulletinBoard
import jp.ac.tmu.sakailab.mqtt.util.MyRG

/**
 * JumpMqtt Broker (proposed solution).
 *
 * Implements the multicast-like optimisation over the Jump Routing protocol:
 *
 * - Phase I  (source broker): receives a single [PubMsg] from the publisher device,
 *   consults the [PublicBulletinBoard] to discover all subscribers, and initiates one
 *   [JumpEncapMsg] per unique destination broker. Multiple subscribers sharing the same
 *   destination broker are served by the same circuit (deduplication via a Set).
 *   The inner payload of each [JumpEncapMsg] is a plain [PubMsg] (not [UnicastPubMsg]),
 *   so that the destination broker can fan out to ALL locally subscribed devices via
 *   the inherited [AMqttBroker.forwardEncapMsgToDevice] which iterates by topicId.
 *
 * - Phase II (relay broker): probabilistic jump forwarding with probability [pf].
 *   If the hop budget is exhausted or the coin-flip fails, the relay switches to Phase III.
 *
 * - Phase III (last relay): forwards the [JumpEncapMsg] directly to the destination broker
 *   identified in the [JumpHeader].
 *
 * - Destination broker: [forwardEncapMsgToDevice] is inherited from [AMqttBroker] and
 *   delivers the [PubMsg] to every locally associated subscriber of the topic.
 */
open class JumpMqttBroker(
    id: Int,
    circuitLength: Int
) : AMqttBroker(id, circuitLength) {
    private val pf: Double = 0.85   // base: 0.85
    private val maxHops: Int = 10

    // Phase I — Source broker: fan-out to one JumpEncapMsg per destination broker

    /**
     * Called when the source broker receives the single encrypted [PubMsg] from the
     * publisher device. This is the core of the JumpMqtt optimisation:
     *
     * 1. Unwrap the [PubMsg] from its Onion layer.
     * 2. Query the [PublicBulletinBoard] for all subscribers of [PubMsg.topicId].
     * 3. For each unique destination broker (skip duplicates via [processedDestBrokers]):
     *    a. Use the original [PubMsg] (first iteration) or a clone (subsequent iterations)
     *       as the inner payload — keeping the topicId intact so the destination broker
     *       can fan out to ALL local subscribers via the inherited
     *       [AMqttBroker.forwardEncapMsgToDevice].
     *    b. Build a [JumpHeader] with jump-routing metadata.
     *    c. Wrap the [PubMsg] in a [JumpEncapMsg] and enqueue it.
     *
     * Complexity on the device side: O(1) — only one radio transmission.
     * Complexity on the source broker: O(D) where D = number of distinct destination brokers.
     *
     * @param msg The [EncapMsg] received from the publisher device (Onion wrapping a [PubMsg]).
     */
    override fun recvEncapMsgFromPublisher(msg: EncapMsg) {
        val pubMsg = msg.getEncapMsg() as PubMsg

        // Trace the forwarding event at the source broker.
        MsgTracer.onForwarded(pubMsg, this.id, Scheduler.time)

        // Retrieve the full set of subscriber device IDs for this topic.
        val destDeviceIds = PublicBulletinBoard.getDestDeviceIds(pubMsg.topicId)
        if (destDeviceIds.isEmpty()) return

        // Track which destination brokers have already been served to avoid duplicate circuits.
        val processedDestBrokers = mutableSetOf<Int>()

        var needCopy = false

        for (destDeviceId in destDeviceIds) {
            val destBrokerId = PublicBulletinBoard.getDestBrokerId(destDeviceId)

            if (destBrokerId in processedDestBrokers) continue // skip if we already created a circuit for this dest broker
            processedDestBrokers.add(destBrokerId)

            // Use the original PubMsg for the first destination broker, and a clone for others
            val innerMsg: PubMsg = if (needCopy) {
                val cloned = pubMsg.cloneMsg() as PubMsg
                MsgTracer.inheritRecord(pubMsg, cloned.getMsgId())
                cloned
            } else {
                pubMsg
            }

            // Build Jump routing header.
            val immediateNext = selectImmediateNeighbor(destBrokerId)
            val jumpTarget = selectJumpTarget(destBrokerId, immediateNext)
            val header = JumpHeader(
                destBrokerId = destBrokerId,
                prevHopId = this.id,
                jumpTargetId = jumpTarget,
                hopCount = 0,
                maxHops = maxHops,
                proof = mockVrfProof(this.id, jumpTarget, destBrokerId)
            )

            // Wrap in a JumpEncapMsg and size it.
            val jumpMsg = JumpEncapMsg(this.id, immediateNext, header, innerMsg).apply {
                setConstantMsgSize(this@JumpMqttBroker.circuitLength)
            }

            MsgTracer.onForwarded(jumpMsg, this.id, Scheduler.time)
            addCpuQueue(jumpMsg as Msg)

            if (Config.isTrace) {
                println("Broker $id (JumpMqtt) initiating circuit to broker $destBrokerId " +
                        "via $immediateNext at ${Scheduler.time}")
            }

            needCopy = true
        }
    }

    // Phase II — Relay broker: probabilistic jump extension
    /**
     * Processes a [JumpEncapMsg] at an intermediate relay broker.
     *
     * If this broker is the destination broker, it delivers the inner message to the
     * subscriber device directly (end of Phase II / beginning of delivery).
     *
     * Otherwise, with probability [pf] (and while below [maxHops]), the relay extends
     * the jump chain. When the chain stops, it switches to Phase III.
     *
     * @param encapMsg The [JumpEncapMsg] received from the previous relay.
     */
    override fun forwardEncapMsgToBroker(encapMsg: EncapMsg) {
        val jumpMsg = encapMsg as JumpEncapMsg
        val header = jumpMsg.getHeader()

        // Simulated VRF proof verification.
        if (!verifyJumpProof(header)) return

        // If this broker is already the destination, deliver to device immediately.
        if (this.id == header.destBrokerId) {
            forwardEncapMsgToDevice(jumpMsg.getEncapMsg() as PubMsg)
            return
        }

        val shouldContinue = header.hopCount < header.maxHops && MyRG.nextDouble() < pf
        if (shouldContinue) {
            // Extend the jump chain: pick a new immediate neighbour and jump target.
            val immediateNext = selectImmediateNeighbor(header.destBrokerId)
            val jumpTarget = selectJumpTarget(header.destBrokerId, immediateNext)
            val nextHeader = header.copy(
                prevHopId = this.id,
                jumpTargetId = jumpTarget,
                hopCount = header.hopCount + 1,
                proof = mockVrfProof(this.id, jumpTarget, header.destBrokerId)
            )

            val nextMsg = JumpEncapMsg(this.id, immediateNext, nextHeader, jumpMsg.getEncapMsg())
            MsgTracer.onForwarded(nextMsg, this.id, Scheduler.time)
            addCpuQueue(nextMsg as Msg)
        } else {
            // Phase III: stop the chain and forward directly to the destination broker.
            forwardToDestinationBroker(jumpMsg)
        }
    }

    // Phase III — Last relay: direct forward to destination broker
    /**
     * Sends the [JumpEncapMsg] directly to [JumpHeader.destBrokerId], bypassing any
     * further relay hops. This is the Phase III direct-delivery step.
     *
     * @param jumpMsg The [JumpEncapMsg] whose hop chain has ended.
     */
    private fun forwardToDestinationBroker(jumpMsg: JumpEncapMsg) {
        val header = jumpMsg.getHeader()
        val inner = jumpMsg.getEncapMsg()

        val finalMsg = JumpEncapMsg(this.id, header.destBrokerId, header, inner)
        MsgTracer.onForwarded(finalMsg, this.id, Scheduler.time)
        addCpuQueue(finalMsg as Msg)

        if (Config.isTrace) {
            println("Broker $id (JumpMqtt Phase III) direct-forwarding to broker " +
                    "${header.destBrokerId} at ${Scheduler.time}")
        }
    }

    // fireDecCompleted — dispatch after decryption
    /**
     * Overrides the base dispatcher to handle the three roles this broker can play:
     *
     * 1. **Source broker**: the decrypted message came from one of its associated publisher
     *    devices → call [recvEncapMsgFromPublisher].
     * 2. **Relay broker**: the decrypted message is a [JumpEncapMsg] in transit
     *    → call [forwardEncapMsgToBroker] (Phase II / III logic).
     * 3. **Destination broker**: the message has reached its final broker
     *    → fall through to [forwardEncapMsgToDevice].
     *
     * @param msg The [Msg] produced after the CPU decryption step.
     */
    override fun fireDecCompleted(msg: Msg) {
        if (Config.isTrace) {
            println("${getNodeName()} (JumpMqtt) peeled off one layer $msg at ${Scheduler.time}")
        }

        // Role 1: message originates from a locally associated publisher device.
        if (associatedDevices.containsKey(msg.getSndrId())) {
            recvEncapMsgFromPublisher(msg as EncapMsg)
            return
        }

        // Role 2: message is a JumpEncapMsg still in transit through the network.
        if (msg is JumpEncapMsg) {
            forwardEncapMsgToBroker(msg)
            return
        }

        // Role 3: message has arrived at the destination broker; deliver to device.
        forwardEncapMsgToDevice(msg.getEncapMsg() as PubMsg)
    }

    // -------------------------------------------------------------------------
    // forwardEncapMsgToDevice: inherited from AMqttBroker
    // -------------------------------------------------------------------------
    // NOT overridden here. AMqttBroker.forwardEncapMsgToDevice(pubMsg) reads
    // pubMsg.topicId, finds all locally associated subscriber devices for that
    // topic, and delivers a unicast Onion to each of them — giving correct
    // multicast-to-all-local-subscribers semantics.

    /**
     * Selects a random immediate neighbour broker to use as the next hop.
     * Falls back to [destBrokerId] if no other neighbour is available.
     */
    private fun selectImmediateNeighbor(destBrokerId: Int): Int {
        val brokerIds = connectedBrokers.keys.toList().filter { it != this.id }
        return MyRG.nextListElement(brokerIds) ?: destBrokerId
    }

    /**
     * Selects a random jump-target broker (distinct from this broker and [immediateNext]).
     * Falls back to [destBrokerId] if no valid candidate exists.
     */
    private fun selectJumpTarget(destBrokerId: Int, immediateNext: Int): Int {
        val brokerIds = connectedBrokers.keys.toList()
            .filter { it != this.id && it != immediateNext }
        return MyRG.nextListElement(brokerIds) ?: destBrokerId
    }

    /**
     * Produces a deterministic mock VRF proof string (no real cryptography).
     */
    private fun mockVrfProof(senderId: Int, jumpTargetId: Int, destBrokerId: Int): String {
        return "vrf-$senderId-$jumpTargetId-$destBrokerId"
    }

    /**
     * Accepts any non-empty proof string (mock verification, no real cryptography).
     */
    private fun verifyJumpProof(header: JumpHeader): Boolean {
        return header.proof.isNotEmpty()
    }
}
