package jp.ac.tmu.sakailab.mqtt.routing.tor

import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.MsgTracer
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.routing.AMqttBroker
import jp.ac.tmu.sakailab.mqtt.routing.EncapMsg
import jp.ac.tmu.sakailab.mqtt.routing.Onion
import jp.ac.tmu.sakailab.mqtt.routing.PubMsg
import jp.ac.tmu.sakailab.mqtt.routing.PublicBulletinBoard
import jp.ac.tmu.sakailab.mqtt.routing.UnicastPubMsg

/**
 * An implementation of Tailored Tor Broker.
 *
 * @author Kazuya Sakai, Ph.D.
 */
open class TorBroker(id: Int, circuitLength: Int) : AMqttBroker(id, circuitLength) {

    /**
     * Handles encapsulation request from a publisher.
     * This is the first IoT broker in the circuit.
     * @param msg A received message.
     */
    override fun recvEncapMsgFromPublisher(msg: EncapMsg) {
        // Retrieve a message inside the onion
        val unicastPubMsg = msg.getEncapMsg() as UnicastPubMsg
        val destDeviceId = unicastPubMsg.destId
        val destBrokerId = PublicBulletinBoard.getDestBrokerId(destDeviceId)

        // construct an onion
        val encapMsg = constructOnion(unicastPubMsg, destBrokerId, this.circuitLength)
        MsgTracer.onForwarded(encapMsg, this.id, Scheduler.time)

        // add a CPU job
        addCpuQueue(encapMsg as Msg)
    }

    /**
     * This function forwards a message to its destination device.
     * Tor is a unicast protocol, and thus, his broker first sees the destination ID of the received EncapMsg.
     * Then, the message is encapsulated and forwarded to the destination device.
     * Note that the key idea of the tailored Tor protocol is "deligation", and so, the destination broker
     * forwards a message to one destination, instated of all devices subscribing a given topic.
     *
     * @param pubMsg An encapsulated message to be forwarded.
     */
    override fun forwardEncapMsgToDevice(pubMsg: PubMsg) {
        // Tor is unicast-based
        val unicastPubMsg = pubMsg as UnicastPubMsg

        // This broker directly forwards a published message to the destination device
        val encapMsg = Onion(this.id, unicastPubMsg.destId, unicastPubMsg).apply {
            setConstantMsgSize(1)
        }

        // add a CPU job
        addCpuQueue(encapMsg as Msg)

        // log
        MsgTracer.onForwarded(encapMsg, this.id, Scheduler.time)
    }
}