package jp.ac.tmu.sakailab.mqtt.routing.jumpmqtt

import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.Event
import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.MsgTracer
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.routing.AMqttDevice
import jp.ac.tmu.sakailab.mqtt.routing.EncapMsg
import jp.ac.tmu.sakailab.mqtt.routing.Onion
import jp.ac.tmu.sakailab.mqtt.routing.PubMsg

/**
 * JumpMqtt Device (proposed solution).
 *
 * Key difference from JumpRoutingDevice:
 * The device sends exactly ONE message to its source broker, regardless of the number of
 * subscribers. The source broker is responsible for consulting the PublicBulletinBoard and
 * initiating one JumpEncapMsg per destination broker (fanout at the broker, not at the device).
 * This saves battery on the IoT sensor by keeping its transmission cost O(1).
 *
 * Inheritance: AMqttDevice -> JumpMqttDevice
 */
class JumpMqttDevice(id: Int) : AMqttDevice(id) {

    /**
     * Generates a single publication message for the given topic and schedules its encryption.
     * Unlike JumpRoutingDevice, there is NO loop over destination device IDs here.
     * A plain [PubMsg] is created (not a [UnicastPubMsg]) because the destination is unknown
     * at device level — the source broker resolves it from the PublicBulletinBoard.
     *
     * @param topicId A topic ID of a message to be published.
     * @param time    The timestamp at which the published message is generated.
     */
    override fun genPubMsg(topicId: Int, time: Long) {
        // Single message creation — no loop over subscribers.
        val msg = PubMsg(this.id, topicId)
        MsgTracer.onCreated(msg, this.id, time)

        // Schedule exactly one encryption event toward the associated broker.
        val params = arrayOf<Any?>(msg)
        val e = Event(time, this::fireEncPubMsg, params)
        Scheduler.addEvent(e)

        if (Config.isTrace) {
            println("${getNodeName()} scheduled a single message generation (JumpMqtt): $msg at $time")
        }
    }

    /**
     * Encapsulates the [PubMsg] in a single [Onion] layer addressed to [associatedBrokerId],
     * then enqueues the result in the CPU queue.
     *
     * Only one encryption layer is applied (device → source broker), matching the A-MQTT
     * device behaviour and keeping the per-device cost minimal.
     *
     * @param param Array whose first element is the [PubMsg] to encrypt.
     */
    override fun fireEncPubMsg(param: Array<Any?>) {
        assert(param.size == 1)
        val msg = param[0] as PubMsg

        // Wrap in a single Onion layer toward the source broker.
        val encapMsg = Onion(this.id, associatedBrokerId, msg as EncapMsg)
        // Constant message size: one encryption layer (same as AMqttDevice).
        encapMsg.setConstantMsgSize(1)

        addCpuQueue(encapMsg as Msg)

        if (Config.isTrace) {
            println("${getNodeName()} encrypted message (JumpMqtt): $encapMsg at ${Scheduler.time}")
        }
    }
}
