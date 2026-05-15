package jp.ac.tmu.sakailab.mqtt.routing.dpmqtt

import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.MsgTracer
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.routing.EncapMsg
import jp.ac.tmu.sakailab.mqtt.routing.Onion
import jp.ac.tmu.sakailab.mqtt.routing.PubMsg

/**
 * The implementation of Differentially Private MQTT (DP-MQTT) Devices.
 *
 * @author Kazuya Sakai, Ph.D.
 */
class DPMqttDevice(id: Int) : AbstractPrivacyEnabledDevice(id) {

    /**
     * Only the destination devices of an MQTT session execute this function.
     * The received messages are kept either legitimate published message list or
     * noise message list for recording delivery states.
     * @param msg A received message.
     */
    override fun fireDecCompleted(msg: Msg) {
        // EncapMsg.getEncapMsg() results are safely cast to PubMsg
        val pubMsg = (msg as EncapMsg).getEncapMsg() as PubMsg
        MsgTracer.onDelivered(pubMsg, this.id, Scheduler.time)

        // Add to the appropriate buffer based on whether it's noise or not
        if (pubMsg.isNoiseMsg()) {
            this.recvNoiseMsgs.add(pubMsg)
        } else {
            this.recvPubMsgs.add(pubMsg)
        }

        if (Config.isTrace) {
            println("${getNodeName()} decrypted/received a message $pubMsg at ${Scheduler.time}")
        }
    }

    /**
     * Generates a fake PubMsg to maintain traffic pattern privacy.
     * IoT device without participating a real MQTT session generates one fake published message
     * every interval.
     * @param params A list of params. This is unused for message publication events.
     */
    fun fireGenFakePubMsg(params: Array<Any?>) {
        // The fake PubMsg has -1 in the topic ID field
        val pubMsg = PubMsg(this.id, -1)

        // Wrap the fake message in an Onion layer
        val encapMsg = Onion(this.id, this.associatedBrokerId, pubMsg).apply {
            // For tx from publisher to broker, the number of onion layers is one
            setConstantMsgSize(1)
            setNoiseMsg()
        }

        // add a CPU job
        addCpuQueue(encapMsg as Msg)

        if (Config.isTrace) {
            println("${getNodeName()} generated a fake PubMsg, $pubMsg at ${Scheduler.time}")
        }
    }
}