package jp.ac.tmu.sakailab.mqtt.routing.plainmqtt

import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.MsgTracer
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.routing.AMqttBroker
import jp.ac.tmu.sakailab.mqtt.routing.EncapMsg
import jp.ac.tmu.sakailab.mqtt.routing.Onion
import jp.ac.tmu.sakailab.mqtt.routing.PubMsg
import jp.ac.tmu.sakailab.mqtt.routing.PublicBulletinBoard

/**
 * An implementation of Plain MQTT broker.
 * The published message is encrypted, but the circuit length is always one, i.e., one broker.
 *
 * @author Kazuya Sakai, Ph.D.
 */
class PlainMqttBroker(id: Int, circuitLength: Int) : AMqttBroker(id, 1) {

    init {
        // In Plain MQTT, the circuit length is always 1.
    }

    override fun fireDecCompleted(msg: Msg) {
        /*
		 * If the message comes from a publisher, call recvEncapMsgFromPublisher(.)
		 * If the message comes from a broker and this broker is a relay broker, then call forwardEncapMsgToBroker(.)
		 * If the message comes from a broker and this broker is the last one, then call forwardEncapMsgToDevice(.)
		 */
        // Retrieve the parameters.
        val msg = msg as EncapMsg
        if (Config.isTrace) {
            println("${getNodeName()} peeled off one layer ${msg} at ${Scheduler.time}")
        }

        if (this.associatedDevices.containsKey((msg as Msg).getSndrId())) {
            val encapMsg = (msg.getEncapMsg() as PubMsg).cloneMsg() as PubMsg

            // A message from a publisher.
            // A special case (this broker has subscribers in its neighbors) must be taken care of.
            this.recvEncapMsgFromPublisher(msg)

        } else {
            // The message from a broker
            // Since there is no intermediate broker, this broker forwards the message to the subscribers.
            forwardEncapMsgToDevice(msg.getEncapMsg() as PubMsg)
        }
    }

    override fun recvEncapMsgFromPublisher(msg: EncapMsg) {
        // Received from a publisher, i.e., this is the first IoT broker
        val pubMsg = msg.getEncapMsg() as PubMsg
        val destDeviceIdSet = PublicBulletinBoard.getDestDeviceIds(pubMsg.topicId)

        // Two different devices may be associated with the same broker,
        // and thus, we will remove duplicated onions.
        val destBrokerIdSet = mutableSetOf<Int>()

        // aad a path trace log
        MsgTracer.onForwarded(pubMsg, this.id, Scheduler.time)

        var needCopy = false // True when more than one destination IoT broker exists.
        for (destDeviceId in destDeviceIdSet) {
            // the destination broker ID
            val destBrokerId = PublicBulletinBoard.getDestBrokerId(destDeviceId)

            // this dstDeviceId is skipped if a copy has already been sent
            if (!destBrokerIdSet.add(destBrokerId)) continue
            destBrokerIdSet.add(destBrokerId)

            // When the destination device is in this broker's neighbors,
            // the message is directly sent to the destination device.
            if (destBrokerId == this.id) {
                super.forwardEncapMsgToDevice(pubMsg)
                continue
            }

            // the published message is copied, since this is a simulation
            val innerMsg = if (needCopy) pubMsg.cloneMsg() as EncapMsg else pubMsg

            // The last layer
            val encapMsg = Onion(this.id, destBrokerId, innerMsg)

            // add a CPU job
            addCpuQueue(encapMsg as Msg)

            // add a log
            if (needCopy) MsgTracer.inheritRecord(pubMsg, encapMsg.getMsgId())

            // a copy of a message needs to be generated for the next destination device
            needCopy = true
        }
    }
}