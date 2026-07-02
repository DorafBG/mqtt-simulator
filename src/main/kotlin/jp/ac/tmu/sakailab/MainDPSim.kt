package jp.ac.tmu.sakailab

import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.gen.*
import jp.ac.tmu.sakailab.mqtt.priv.LaplaceMechanism
import jp.ac.tmu.sakailab.mqtt.routing.PublicBulletinBoard
import jp.ac.tmu.sakailab.mqtt.routing.dpmqtt.DPMqttBroker
import jp.ac.tmu.sakailab.mqtt.routing.dpmqtt.NoiseMsgGenerator.generateFakePubMsgs
import jp.ac.tmu.sakailab.mqtt.routing.dpmqtt.NoiseMsgGenerator.generateNoiseMsgs
import jp.ac.tmu.sakailab.mqtt.routing.privmsg.VuvuzelaBroker
import jp.ac.tmu.sakailab.mqtt.MsgTracer
import jp.ac.tmu.sakailab.mqtt.routing.privmsg.VuvuzelaNoiseMsgGenerator.generateFakeVuvuzelaPubMsgs
import jp.ac.tmu.sakailab.mqtt.routing.privmsg.VuvuzelaNoiseMsgGenerator.generateVuvuzelaNoiseMsgs
import jp.ac.tmu.sakailab.mqtt.sim.DPSimulator
import jp.ac.tmu.sakailab.mqtt.util.MyRG
import java.io.File
import java.io.IOException

/**
 * Default simulation parameters.
 */
var numCBR = 1
var numTopics = 100
var numSubscTopics = 10
var numPubMsgs = 1
var txQueueSize = 128 * 1024
var cpuQueueSize = 128 * 1024

/**
 * Sets parameters based on the simulation scenario.
 */
fun setParam(scenarioStr: String) {
    when (scenarioStr) {
        "CIR", "EPS", "DELTA" -> {
            numCBR = 100
            numTopics = 100
            numSubscTopics = 10
            numPubMsgs = 1
            txQueueSize = 128 * 1024
            cpuQueueSize = 128 * 1024
        }
        "INTVL" -> {
            numCBR = 100
            numTopics = 100
            numSubscTopics = 10
            numPubMsgs = 10
            txQueueSize = 1024
            cpuQueueSize = 1024
        }
        "NOISE999" -> {
            numCBR = 999
            numTopics = 100
            numSubscTopics = 10
            numPubMsgs = 1
            txQueueSize = 128 * 1024
            cpuQueueSize = 128 * 1024
        }
        "NOISE100" -> {
            numCBR = 100
            numTopics = 100
            numSubscTopics = 10
            numPubMsgs = 1
            txQueueSize = 128 * 1024
            cpuQueueSize = 128 * 1024
        }
        else -> {
            numCBR = 1
            numTopics = 100
            numSubscTopics = 10
            numPubMsgs = 1
            txQueueSize = 128 * 1024
            cpuQueueSize = 128 * 1024
        }
    }
}

/**
 * Main entry point for the simulation.
 * @author Kazuya Sakai, Ph.D.
 */
fun main(args: Array<String>) {
    if (args.size != 9) {
        ("Invalid number of args: ${args.size}")
        System.exit(1)
    }

    val config = Config

    // 1. Initialize configuration from command line arguments
    config.apply {
        scenario = args[0]
        protName = args[1]
        numBrokers = args[2].toInt()
        numDevices = args[3].toInt()
        cirLength = args[4].toInt()
        pubMsgIntvl = args[5].toLong() * 1000_000_000L // nano sec order
        epsilon = args[6]
        delta = args[7].toDouble()
        seed = args[8].toInt()
    }

    // 2. Initialize simulation environment and parameter verification.
    setParam(config.scenario)
    MyRG.init(config.seed)
    if (!config.verifyParams()) System.exit(1)

    /*
     * IoT system setup:
     * 1. Generate brokers and devices.
     * 2. Bind radio interfaces and set queue sizes.
     * 3. Establish network topology and associations.
     * 4. Assign topic IDs and generate CBR traffic.
     */
    config.brokerList = generateBrokers(config.numBrokers, Config.BROKER_IMPL[config.protName]!!, config.cirLength)
    config.deviceList = generateDevices(config.numDevices, config.numBrokers, Config.DEVICE_IMPL[config.protName]!!)
    config.setNodeList()

    setRadioInterfaces(config.brokerList!!, config.deviceList!!)
    setTxQueueSize(txQueueSize, config.brokerList!!, config.deviceList!!)
    setCpuQueueSize(cpuQueueSize, config.brokerList!!, config.deviceList!!)
    connectBrokers(config.brokerList!!)
    association(config.brokerList!!, config.deviceList!!)
    setTopic(numTopics, numSubscTopics, config.deviceList!!)
//    setCryptoModel(config.nodesList, CryptoDelayModel.LINEAR_CRYPTO_MODEL)

    config.cbrList = generateDeterministicCBR(config.deviceList!!, numCBR, numTopics, numPubMsgs, config.pubMsgIntvl)

    // 3. Initialize the public bulletin board
    PublicBulletinBoard.initDestDeviceIdsTable(numTopics)
    PublicBulletinBoard.initDestBrokerIdTable(numTopics)

    /*
     * Protocol dependent setups (Differential Privacy / Vuvuzela)
     */
    when (config.protName) {
        "DPMQTT", "OptDPMQTT" -> {
            config.brokerList?.forEach { broker ->
                val sensitivity = PublicBulletinBoard.getSensitivity(broker).toDouble()
                (broker as DPMqttBroker).privacyMech = LaplaceMechanism(config.epsilon, config.delta, sensitivity)
            }
            val numNoises: Int = generateNoiseMsgs(config.pubMsgIntvl, numPubMsgs, numTopics, config.brokerList!!)
            val numFakePubs: Int = generateFakePubMsgs(config.pubMsgIntvl, numPubMsgs, config.deviceList!!, config.cbrList!!)
        }
        "Vuvuzela" -> {
            config.brokerList?.forEach { broker ->
                val sensitivity = PublicBulletinBoard.getSensitivity(broker).toDouble()
                (broker as VuvuzelaBroker).privacyMech = LaplaceMechanism(config.epsilon, config.delta, sensitivity)
            }
            val numNoises: Int = generateVuvuzelaNoiseMsgs(config.pubMsgIntvl, numPubMsgs, numTopics, config.brokerList!!)
            val numFakePubs: Int = generateFakeVuvuzelaPubMsgs(config.pubMsgIntvl, numPubMsgs, config.deviceList!!, config.cbrList!!)
        }
    }

    // 4. Run the simulation
    DPSimulator.start()

    // 5. Output results
    val results = config.correctedResults

    // Compute average cryptographic operations per broker
    val avgCryptoOpsPerBroker = if (config.numBrokers > 0) {
        (config.numEnc + config.numDec).toDouble() / config.numBrokers
    } else 0.0

    println(results.toString())

    try {
        val fileName = "result.txt"
        val content = "${getParamsStr(Config.scenario)}${results.joinToString(" ", prefix = " ")} $avgCryptoOpsPerBroker ${MsgTracer.getAverageHops()}\n"

        // file output
        File(fileName).appendText(content)


    } catch (e: IOException) {
        println("Critical I/O Hazard: ${e.message}")
    }
    // A simulation completed
}

/**
 * Generates a string representation of the simulation parameters for logging.
 */
private fun getParamsStr(tag: String): String {
    val config = Config

    // a formatted string with 2-digit for epsilon, e.g, ln2 => ln02
    var epsilonString = config.epsilon
    if (config.epsilon.startsWith("ln")) {
        epsilonString = "ln" + config.epsilon.substring(2).toInt().toString().padStart(2, '0')
    }

    // a formatted string with 5-digit for delta, e.g., 0.00001 => 00001
    var deltaString = (config.delta * 10000).toInt().toString().padStart(5, '0')

    return "${tag}_${config.protName}_${config.numBrokers}_${config.numDevices}_" +
            "${epsilonString}_${deltaString}_" +
            "${config.cirLength}_${config.pubMsgIntvl.toString().padStart(5, '0')}" +
            " " + "${config.seed}"
}