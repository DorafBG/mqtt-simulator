package jp.ac.tmu.sakailab.mqtt.routing.dpmqtt

import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.MsgTracer
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.routing.*
import jp.ac.tmu.sakailab.mqtt.util.MyRG

/**
 * The implementation of Differentially Private MQTT (DP-MQTT) Brokers.
 *
 * @author Kazuya Sakai, Ph.D.
 */
open class DPMqttBroker(id: Int, circuitLength: Int) : AbstractPrivacyEnabledBroker(id, circuitLength) {
    /**
     * This function is called when a decryption operation is completed.
     * When a received message comes from a publisher (i.e., device), then it will be discarded.
     * This is because the fake published message (noise) from a device is local, i.e., it is sufficient
     * to deliver the noise to its associated broker.
     * Otherwise, this broker forwards an encapsulated message to the next broker as the AMqttBroker does.
     *
     * @param params A list of parameters, [[EncapMsg]]
     */
    override fun fireDecCompleted(msg: Msg) {
        // Retrieve the parameters using the array index
        if ((msg as EncapMsg).isFakePubMsg()) {
            // Drop fake PubMsg silently to maintain differential privacy
            if (Config.isTrace) {
                println("${getNodeName()} decrypted and dropped a fake PubMsg $msg at ${Scheduler.time}.")
            }
        } else {
            // Call back to AMQTT processing for real messages
            super.fireDecCompleted(msg)
        }
    }

    /**
     * Each broker generates noise messages every interval.
     * The interval is determined by the message publication interval, such as one second and 1 minute.
     * @param params A list of parameters, i.e., [[the topic ID range]]
     */
     override fun fireGenNoiseMsgs(params: Array<Any?>) {
        assert(params.size == 1)
        val topicIdRange = params[0] as Int // Retrieve the parameters

        // Retrieve number of noises from the privacy mechanism
        val numNoise = privacyMech?.getNumNoiseMsgs() ?: 0
        if (Config.isTrace) println("${getNodeName()} generated noises. numNoise = $numNoise")

        repeat(numNoise) {
            // Randomly select topicID and destination
            val topicId = MyRG.nextInt(topicIdRange)
            val destDeviceIdList = PublicBulletinBoard.destDeviceIdTable[topicId]?.toList() ?: listOf()

            // chose a different topic ID if no device is subscribing the selected topic.
            if (destDeviceIdList.isEmpty()) return@repeat

            // Select one of the destination devices for this noise.
            val destDeviceId = MyRG.nextListElement(destDeviceIdList) ?:
                throw IllegalStateException("ERROR (DPMqttBroker.kt): The list of destination device IDs is empty.")
            val destBrokerId = PublicBulletinBoard.getDestBrokerId(destDeviceId)

            // construct an onion
            var noiseMsg: PubMsg = PubMsg(this.id, topicId).apply { setNoiseMsg() }
            val encapMsg = constructOnion(noiseMsg, destBrokerId, this.circuitLength).apply {
                setNoiseMsg() // For the simulation purpose, this message is denoted as a noise.
                setConstantMsgSize(circuitLength)
            }

            // add a CPU job
            addCpuQueue(encapMsg as Msg)
        }
    }

    /**
     * This forwards a published message to the subscribers in this broker's neighbors.
     * Note that all the subscribers in the neighbors will receive the message, since this broker
     * knows only the topic ID as the ultimate destination, but not destination device ID.
     * @param pubMsg A published message instance.
     */
    override fun forwardEncapMsgToDevice(pubMsg: PubMsg) {
        val destDeviceIdSet = getDestIdSetInAssociatedDevices(pubMsg.topicId)
        if (Config.isTrace) {
            val type = if (pubMsg.isNoiseMsg()) "noise message" else "message"
            println("${getNodeName()} started broadcasting a $type to $destDeviceIdSet at ${Scheduler.time}")
        }

        // Add a path trace log only for legitimate messages, but not noise messages.
        MsgTracer.onForwarded(pubMsg, this.id, Scheduler.time)

        var needCopy = false
        for (deviceId in destDeviceIdSet) {
            // Clone msg if there are multiple recipients
            val innerMsg = if (needCopy) pubMsg.cloneMsg() as PubMsg else pubMsg

            // construct an onion
            val encapMsg = Onion(this.id, deviceId, innerMsg).apply {
                if (innerMsg.isNoiseMsg()) setNoiseMsg()
                setConstantMsgSize(1)
            }

            // add a CPU job
            addCpuQueue(encapMsg as Msg)

            // add log
            if (needCopy) MsgTracer.inheritRecord(pubMsg, encapMsg.getMsgId())

            // a copy of a message needs to be generated for the next destination device
            needCopy = true
        }
    }
}