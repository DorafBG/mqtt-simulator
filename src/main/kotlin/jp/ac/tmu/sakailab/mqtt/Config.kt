package jp.ac.tmu.sakailab.mqtt

import jp.ac.tmu.sakailab.numCBR
import kotlin.collections.contains

/**
 * Configuration class for simulations.
 * @auhor Kazuya Sakai, Ph.D.
 */
object Config {
    var isTrace = false

    /**
     * The paths to the class of each protocol.
     */
    val BROKER_IMPL: Map<String, String> = mapOf(
        "PlainMQTT" to "jp.ac.tmu.sakailab.mqtt.routing.plainmqtt.PlainMqttBroker",
        "Tor" to "jp.ac.tmu.sakailab.mqtt.routing.tor.TorBroker",
        "AMQTT" to "jp.ac.tmu.sakailab.mqtt.routing.AMqttBroker",
        "Vuvuzela" to "jp.ac.tmu.sakailab.mqtt.routing.privmsg.VuvuzelaBroker",
        "DPMQTT" to "jp.ac.tmu.sakailab.mqtt.routing.dpmqtt.DPMqttBroker",
        "OptDPMQTT" to "jp.ac.tmu.sakailab.mqtt.routing.dpmqtt.OptDPMqttBroker"
    )

    /**
     * The paths to the class of each protocol.
     */
    val DEVICE_IMPL: Map<String, String> = mapOf(
        "PlainMQTT" to "jp.ac.tmu.sakailab.mqtt.routing.plainmqtt.PlainMqttDevice",
        "Tor" to "jp.ac.tmu.sakailab.mqtt.routing.tor.TorDevice",
        "AMQTT" to "jp.ac.tmu.sakailab.mqtt.routing.AMqttDevice",
        "Vuvuzela" to "jp.ac.tmu.sakailab.mqtt.routing.privmsg.VuvuzelaDevice",
        "DPMQTT" to "jp.ac.tmu.sakailab.mqtt.routing.dpmqtt.DPMqttDevice",
        "OptDPMQTT" to "jp.ac.tmu.sakailab.mqtt.routing.dpmqtt.DPMqttDevice"
    )

    // The system and protocol parameters
    var scenario: String = "SIM"
    var protName: String? = null
    var numBrokers: Int = 0
    var numDevices: Int = 0
    var cirLength: Int = 0 // the onion length
    var pubMsgIntvl: Long = 0 // 1~60 seconds, i.e., 1 sec ~ 1 min
    var epsilon: String = "ln2" // double values or string such as ln2 and ln3 can be set
    var delta: Double = 0.0
    var seed: Int = 0

    // IoT system information
    var nodesList: MutableList<Node> = mutableListOf()
    var brokerList: MutableList<Broker>? = null
    var deviceList: MutableList<Device>? = null
    var cbrList: MutableList<ConstBitRate>? = null

    // statistics for results
    var numInitMsg = 0
    var numDeliMsg = 0
    var delivery = 0.0
    var delay = 0.0
    var numMsgTx = 0
    var amntMsgTrfc = 0L
    var numNoiseTx = 0
    var amntNoiseTrfc = 0L
    var totalTx = 0 // this includes both legitimate messages and noise messages.
    var totalAmntTrfc = 0L
    var numEnc = 0
    var numDec = 0
    var encDelay = 0.0
    var decDelay = 0.0

    // for verification purpose
    var numDeliNoiseMsg = 0

    val correctedResults: List<Double>
        get() = listOf(
            numInitMsg.toDouble(),
            numDeliMsg.toDouble(),
            delivery,
            delay / 1000_000.0, // the millisecond order
            numMsgTx.toDouble(),
            amntMsgTrfc.toDouble(),
            numNoiseTx.toDouble(),
            amntNoiseTrfc.toDouble(),
            totalTx.toDouble(),
            totalAmntTrfc.toDouble(),
            numEnc.toDouble(),
            numDec.toDouble(),
            encDelay / 1000_000.0, // the millisecond order
            decDelay / 1000_000.0, // the millisecond order
        )

    val correctedResultsString: List<String>
        get() = listOf(
            "numInitMsg", "numDeliMsg", "delivery", "delay",
            "numMsgTx", "amntMsgTrfc", "numNoiseTx", "amntNoiseTrfc",
            "totalTx", "totalAmntTrfc",
            "numEnc", "numDec", "encDelay", "decDelay"
        )

    /**
     * This method verifies the input parameters are valid.
     * The current limitations:
     * - The circuit length >= 3 must hold (i.e., the number of intermediate brokers >= 3).
     */
    fun verifyParams(): Boolean {
        /**
         * The protocol names.
         */
        if (protName !in listOf("PlainMQTT", "Tor", "AMQTT", "DPMQTT", "OptDPMQTT", "Vuvuzela")) {
            System.err.println("Invalid protocol name, ${protName}.")
            System.err.flush()
            return false
        }

        /**
         * A combination of parameters.
         */
        if (numCBR > numDevices) {
            System.err.println("Invalid parameter combination (numCBR and numDevices) : ${numCBR} > ${numDevices}")
            System.err.flush()
            return false
        }

        // Parameters.
        if (cirLength < 3) {
            println("The circuit length must be greater than or equal to 3 except PlainMQTT.")
            return false
        }

        // the input parameters are correct
        return true
    }

    /**
     * Aggregates brokers and devices into a single nodesList.
     */
    fun setNodeList() {
        val bList = brokerList
        val dList = deviceList

        if (bList != null && dList != null) {
            // Explicitly specify <Node> to avoid inference as MutableList<Any>
            val combinedList: List<Node> = (bList as List<Node>) + (dList as List<Node>)
            nodesList = combinedList.toMutableList()

            if (isTrace) {
                println("INFO: Nodes aggregation completed. Total nodes: ${nodesList.size}")
            }
        } else {
            // Using error output stream for system errors
            System.err.println("ERROR: Config.kt does not have broker or device list.")
        }
    }
}