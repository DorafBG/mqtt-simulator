package jp.ac.tmu.sakailab.mqtt.routing.privmsg

import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.MsgTracer
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.routing.*
import jp.ac.tmu.sakailab.mqtt.util.MyRG
import jp.ac.tmu.sakailab.mqtt.routing.dpmqtt.AbstractPrivacyEnabledBroker

/*
 * The Vuvuzela IoT Broker class.
 *
 * @author Kazuya Sakai, Ph.D.
 */
class VuvuzelaBroker(id: Int, circuitLength: Int) : AbstractPrivacyEnabledBroker(id, circuitLength) {

    override fun fireDecCompleted(msg: Msg) {
        // Vuvuzela logic: drop fake messages at the broker layer
        if ((msg as EncapMsg).isFakePubMsg()) {
            if (Config.isTrace) {
                println("${getNodeName()}  decrypted and dropped a fake PubMsg $msg at ${Scheduler.time}.")
            }
        } else {
            // Call back to AMQTT processing for real messages
            super.fireDecCompleted(msg)
        }
    }

    /**
     * Generates Vuvuzela-specific noise messages with full onion routing.
     * @param params A list of params, i.e., [[the number of topic IDs]]
     */
     override fun fireGenNoiseMsgs(params: Array<Any?>) {
        assert(params.size == 1)
        val topicIdRange = params[0] as Int // Retrieve the parameters

        // Retrieve number of noises from the privacy mechanism
        val numNoise = privacyMech?.getNumNoiseMsgs() ?: 0
        if (Config.isTrace) println("Broker $id generates noises. numNoise = $numNoise")

        repeat(numNoise) {
            // Randomly select topicID and destination
            val topicId = MyRG.nextInt(topicIdRange)
            val destDeviceIdList = PublicBulletinBoard.destDeviceIdTable[topicId]?.toList() ?: listOf()

            // chose a different topic ID if no device is subscribing the selected topic.
            if (destDeviceIdList.isEmpty()) return@repeat

            // Select one of the destination device for this noise.
            val destDeviceId = MyRG.nextListElement(destDeviceIdList) ?:
            throw IllegalStateException("ERROR (VuvuzelaBroker.kt): The list of destination device IDs is empty.")
            val destBrokerId = PublicBulletinBoard.getDestBrokerId(destDeviceId)

            // construct an onion
            var noiseMsg: PubMsg = UnicastPubMsg(this.id, topicId, destDeviceId).apply { setNoiseMsg() }
            val encapMsg = constructOnion(noiseMsg, destBrokerId, this.circuitLength)
            encapMsg.setNoiseMsg() // For the simulation purpose, this message is denoted as a noise.

            // add a CPU job
            addCpuQueue(encapMsg as Msg)
        }
    }

    override fun recvEncapMsgFromPublisher(msg: EncapMsg) {
        // Retrieve a message inside the onion
        val unicastPubMsg = msg.getEncapMsg() as UnicastPubMsg
        val destDeviceId = unicastPubMsg.destId
        val destBrokerId = PublicBulletinBoard.getDestBrokerId(destDeviceId)

        // construct an onion
        val encapMsg = constructOnion(unicastPubMsg, destBrokerId, this.circuitLength).apply {
            if (unicastPubMsg.isNoiseMsg()) setNoiseMsg() // this is for a simulation purpose
            setConstantMsgSize(circuitLength)
        }
        MsgTracer.onForwarded(encapMsg, this.id, Scheduler.time)

        // add a CPU job
        addCpuQueue(encapMsg as Msg)
    }

    /**
     * By this function, this broker forwards a published message to devices which are subscribing
     * the message topic within this broker's neighbors.
     * @param pubMsg A published message, which is a UnicastPubMsg instance.
     */
    override fun forwardEncapMsgToDevice(pubMsg: PubMsg) {
        val destDeviceIdSet = getDestIdSetInAssociatedDevices(pubMsg.topicId)
        val unicastPubMsg = pubMsg as UnicastPubMsg // UnicastPubMsg

        if (Config.isTrace) {
            val type = if (pubMsg.isNoiseMsg()) "noise message" else "message"
            println("${getNodeName()} start forwarding a $type to ${unicastPubMsg.destId} at ${Scheduler.time}")
        }

        // Add a path trace log only for legitimate messages, but not noise messages.
        MsgTracer.onForwarded(pubMsg, this.id, Scheduler.time)

        // a message will be forwarded to all devices which subscribe a given topic in this broker's neighbor
        // Note: unlike Tailored Tor, a Vuvuzela broker does not delegate a device
        for (deviceId in destDeviceIdSet) {
            // the original copy is kept for the destination device.
            val innerMsg = if (deviceId != unicastPubMsg.destId) pubMsg.cloneMsg() as PubMsg else pubMsg

            // encapsulate a message
            val encapMsg = Onion(this.id, deviceId, innerMsg).apply {
                if (innerMsg.isNoiseMsg()) setNoiseMsg() // this is for a simulation purpose
                setConstantMsgSize(circuitLength)
            }

            // add a CPU job
            addCpuQueue(encapMsg as Msg)

            // do not use MsgTracer for copied message, which will be forwarded to non-designated destination.
        }
    }
}