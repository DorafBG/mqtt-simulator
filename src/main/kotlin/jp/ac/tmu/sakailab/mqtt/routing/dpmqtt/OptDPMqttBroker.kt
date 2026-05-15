package jp.ac.tmu.sakailab.mqtt.routing.dpmqtt

import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.routing.Onion
import jp.ac.tmu.sakailab.mqtt.routing.PubMsg
import jp.ac.tmu.sakailab.mqtt.routing.PublicBulletinBoard
import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.util.MyRG

/**
 * Optimized DP-MQTT optimizes the number of noise messages by opportunistic distinguishability.
 *
 * @author Kazuya Sakai, Ph.D.
 */
class OptDPMqttBroker(id: Int, circuitLength: Int) : DPMqttBroker(id, circuitLength) {

    /**
     * This function generates noise messages.
     * Unlike DP-MQTT, Optimized DP-MQTT brokers generate noise messages with one hop onion circuit,
     * which ensures anonymity by opportunistic distinguishability.
     *
     * @param params A list of parameters, i.e., [[the number of topic IDs]]
     */
    override fun fireGenNoiseMsgs(params: Array<Any?>) {
        assert(params.size == 1)
        val topicIdRange = params[0] as Int // Retrieve the parameters

        // Access the privacy mechanism inherited from DPMqttBroker
        val numNoise = this.privacyMech?.getNumNoiseMsgs() ?: 0
        if (Config.isTrace) println("Broker $id generates noises (optimized). numNoise = $numNoise")

        repeat(numNoise) {
            // Randomly select topicID and destination
            val topicId = MyRG.nextInt(topicIdRange)
            val destDeviceIdList = PublicBulletinBoard.destDeviceIdTable[topicId]?.toList() ?: listOf()

            // If no device subscribes a chosen topic, then noise generation is skipped.
            if (destDeviceIdList.isEmpty()) return@repeat

            // Select one of the destination device for this noise.
            val destDeviceId = MyRG.nextListElement(destDeviceIdList) ?:
            throw IllegalStateException("ERROR (OptDPMqttBroker.kt): The list of destination device IDs is empty.")
            val destBrokerId = PublicBulletinBoard.getDestBrokerId(destDeviceId)

            // Create a noise message with only one layer of onion
            val innerMsg = PubMsg(this.id, topicId).apply { setNoiseMsg() }

            // When the destination device is associated with this broker, then a noise is directly sent to the device.
            if (destBrokerId == this.id) {
                super.forwardEncapMsgToDevice(innerMsg)
                return@repeat
            }

            // Encapsulate the noise message toward the destination broker.
            val encapMsg = Onion(this.id, destBrokerId, innerMsg).apply {
                setNoiseMsg()
                setConstantMsgSize(circuitLength)
            }

            // add a CPU job
            addCpuQueue(encapMsg as Msg)
        }
    }
}