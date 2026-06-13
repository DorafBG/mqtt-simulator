package jp.ac.tmu.sakailab.mqtt.routing.jumpmqtt

import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.MsgTracer
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.routing.AMqttBroker
import jp.ac.tmu.sakailab.mqtt.routing.EncapMsg
import jp.ac.tmu.sakailab.mqtt.routing.PubMsg
import jp.ac.tmu.sakailab.mqtt.routing.PublicBulletinBoard
import jp.ac.tmu.sakailab.mqtt.routing.UnicastPubMsg
import jp.ac.tmu.sakailab.mqtt.util.MyRG
import jp.ac.tmu.sakailab.mqtt.routing.Onion

/**
 * Jump-Routing broker.
 * Phase I: source broker creates JumpEncapMsg per destination.
 * Phase II: relay brokers verify jump, extend or stop.
 * Phase III: last relay reveals destination broker and forwards directly.
 */
open class JumpRoutingBroker(
    id: Int,
    circuitLength: Int
) : AMqttBroker(id, circuitLength) {
    private val pf: Double = 0.85 // base:  0.85
    private val maxHops: Int = 10 // base: 10

    /**
     * Phase I: source broker initiation.
     */
    override fun recvEncapMsgFromPublisher(msg: EncapMsg) {
        val unicastPubMsg = msg.getEncapMsg() as UnicastPubMsg
        val destBrokerId = PublicBulletinBoard.getDestBrokerId(unicastPubMsg.destId)

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

        val jumpMsg = JumpEncapMsg(this.id, immediateNext, header, unicastPubMsg).apply {
            setConstantMsgSize(this@JumpRoutingBroker.circuitLength)
        }

        MsgTracer.onForwarded(jumpMsg, this.id, Scheduler.time)
        addCpuQueue(jumpMsg as Msg)
    }

    /**
     * Phase II: relay broker processing.
     */
    override fun forwardEncapMsgToBroker(encapMsg: EncapMsg) {
        val jumpMsg = encapMsg as JumpEncapMsg
        val header = jumpMsg.getHeader()

        // Simulated jump verification.
        if (!verifyJumpProof(header)) return

        // If this broker is the destination, deliver to the target device.
        if (this.id == header.destBrokerId) {
            forwardEncapMsgToDevice(jumpMsg.getEncapMsg() as PubMsg)
            return
        }

        val shouldContinue = header.hopCount < header.maxHops && MyRG.nextDouble() < pf
        if (shouldContinue) {
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
            forwardToDestinationBroker(jumpMsg)
        }
    }

    /**
     * Phase III: final relay forwards directly to destination broker.
     */
    private fun forwardToDestinationBroker(jumpMsg: JumpEncapMsg) {
        val header = jumpMsg.getHeader()
        val inner = jumpMsg.getEncapMsg()

        val finalMsg = JumpEncapMsg(this.id, header.destBrokerId, header, inner)
        MsgTracer.onForwarded(finalMsg, this.id, Scheduler.time)
        addCpuQueue(finalMsg as Msg)
    }

    /**
     * Override broker dispatch to treat JumpEncapMsg as an onion for relay processing,
     * and deliver unicast to the destination device in Phase III.
     */
    override fun fireDecCompleted(msg: Msg) {
        if (associatedDevices.containsKey(msg.getSndrId())) {
            recvEncapMsgFromPublisher(msg as EncapMsg)
            return
        }

        if (msg is JumpEncapMsg) {
            forwardEncapMsgToBroker(msg)
            return
        }

        forwardEncapMsgToDevice(msg.getEncapMsg() as PubMsg)
    }

    /**
     * Destination broker -> device (unicast).
     */
    override fun forwardEncapMsgToDevice(pubMsg: PubMsg) {
        val unicastPubMsg = pubMsg as UnicastPubMsg
        val encapMsg = Onion(this.id, unicastPubMsg.destId, unicastPubMsg).apply {
            setConstantMsgSize(1)
        }

        addCpuQueue(encapMsg as Msg)
        MsgTracer.onForwarded(encapMsg, this.id, Scheduler.time)
    }

    private fun selectImmediateNeighbor(destBrokerId: Int): Int {
        val brokerIds = connectedBrokers.keys.toList().filter { it != this.id }
        return MyRG.nextListElement(brokerIds) ?: destBrokerId
    }

    private fun selectJumpTarget(destBrokerId: Int, immediateNext: Int): Int {
        val brokerIds = connectedBrokers.keys.toList()
            .filter { it != this.id && it != immediateNext }
        return MyRG.nextListElement(brokerIds) ?: destBrokerId
    }

    private fun mockVrfProof(senderId: Int, jumpTargetId: Int, destBrokerId: Int): String {
        // Mock VRF proof: deterministic string, no real crypto.
        return "vrf-$senderId-$jumpTargetId-$destBrokerId"
    }

    private fun verifyJumpProof(header: JumpHeader): Boolean {
        // Mock verification: accept non-empty proof.
        return header.proof.isNotEmpty()
    }
}

