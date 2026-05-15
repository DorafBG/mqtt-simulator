package jp.ac.tmu.sakailab.mqtt.gen

import jp.ac.tmu.sakailab.mqtt.*
import jp.ac.tmu.sakailab.mqtt.crypto.ConstantCryptoModel
import jp.ac.tmu.sakailab.mqtt.crypto.CryptoDelayModel
import jp.ac.tmu.sakailab.mqtt.crypto.LinearCryptoModel
import jp.ac.tmu.sakailab.mqtt.radio.NICImpl
import jp.ac.tmu.sakailab.mqtt.util.MyRG

/**
 * IoT system generator.
 * @author Kazuya Sakai, Ph.D.
 */

/**
 * A set of broker instances are generated using "Reflections".
 * That is class name is given by a String, e.g., "jp.ac.tmu.sakailab.mqtt.routing.AMQTTBroker",
 * and the corresponding class instances are instantiated.
 */
fun generateBrokers(numBrokers: Int, protName: String, circuitLength: Int): MutableList<Broker> {
    val list = mutableListOf<Broker>()
    val clazz = Class.forName(protName)
    val constructor = clazz.getConstructor(Int::class.java, Int::class.java)

    repeat(numBrokers) { i ->
        list.add(constructor.newInstance(i, circuitLength) as Broker)
    }
    return list
}

/**
 * A set of device instances are generated using "Reflections".
 * That is class name is given by a String, e.g., "jp.ac.tmu.sakailab.mqtt.routing.AMQTTDevice",
 * and the corresponding class instances are instantiated.
 *
 * @param numDevices The number of devices to be generated.
 * @param numBrokers The number of brokers to be generated.
 * @param protName The protocol name to be instantiated.
 */
fun generateDevices(numDevices: Int, numBrokers: Int, protName: String): MutableList<Device> {
    val list = mutableListOf<Device>()
    val clazz = Class.forName(protName)
    val constructor = clazz.getConstructor(Int::class.java)

    for (i in numBrokers until (numBrokers + numDevices)) {
        list.add(constructor.newInstance(i) as Device)
    }
    return list
}

/**
 * Brokers are connected by a complete graph.
 *
 * @param brokerList A list of brokers.
 */
fun connectBrokers(brokerList: List<Broker>) {
    for (i in 0 until brokerList.size - 1) {
        for (j in i + 1 until brokerList.size) {
            val b1 = brokerList[i] as Node
            val b2 = brokerList[j] as Node
            b1.addNbr(b2)
            b2.addNbr(b1)
            (b1 as Broker).setConnectedBrokers(b2 as Broker)
            (b2 as Broker).setConnectedBrokers(b1 as Broker)
        }
    }
}

/**
 * Devices are associated with Brokers.
 *
 * @param brokerList A list of brokers.
 * @param   deviceList A list of devices.
 */
fun association(brokerList: List<Broker>, deviceList: List<Device>) {
    deviceList.forEachIndexed { i, d ->
        val j = i % brokerList.size
        val b = brokerList[j]

        b.setAssociatedDevice(d)
        d.associatedBroker = b

        (b as Node).addNbr(d as Node)
        (d as Node).addNbr(b as Node)
    }
}

/**
 * Topic IDs are randomly assigned to devices.
 *
 * @param numTopics The number of topics.
 * @param deviceList A list of devices.
 */
fun setTopic(numTopics: Int, avgTopics: Int, deviceList: List<Device>) {
    deviceList.forEach { d ->
        var cnt = 0
        while (cnt < avgTopics) {
            val t = MyRG.nextInt(numTopics)
            if (!d.subscTopics.contains(t)) {
                d.setSubscTopic(t)
                cnt++
            }
        }
    }
}

/**
 * Broker and device instances and NICs are bound.
 *
 * @param brokerList A list of brokers.
 * @param deviceList A list of devices.
 * @param brokerTxRate The transmission rate among brokers with the default value being 1Gbps.
 * @param deviceTxRate The transmission rate between a device and a broker with the default value being 256Mbps (Mobile).
 */
fun setRadioInterfaces(
    brokerList: List<Broker>,
    deviceList: List<Device>) {
    brokerList.forEach { NICImpl.bindCoreNICInterface(it as Node) }
    deviceList.forEach { NICImpl.bindSensorNICInterface(it as Node) }
}

/**
 * Brokers and devices' transmission queue size are initialized.
 *
 * @param queueSize The queue size.
 * @param brokerList A list of brokers.
 * @param deviceList A list of devices.
 */
fun setTxQueueSize(queueSize: Int, brokerList: List<Broker>, deviceList: List<Device>) {
    brokerList.forEach { (it as Node).txQueueSize = queueSize }
    deviceList.forEach { (it as Node).txQueueSize = queueSize }
}

/**
 * Brokers and devices' CPU queue size are initialized.
 *
 * @param queueSize The queue size.
 * @param brokerList A list of brokers.
 * @param deviceList A list of devices.
 */
fun setCpuQueueSize(queueSize: Int, brokerList: List<Broker>, deviceList: List<Device>) {
    brokerList.forEach { (it as Node).cpuQueueSize = queueSize }
    deviceList.forEach { (it as Node).cpuQueueSize = queueSize }
}

/**
 * A set of constant bit rate (CBR) traffic instances are randomly generated.
 *
 * @param deviceList The list of devices participating in the simulation.
 * @param numCBR The number of CBR traffic flows to generate.
 * @param numTopics The total number of available topics.
 * @param numMsgs The number of messages per CBR flow.
 * @param intvl The interval between message generations.
 */
fun generateCBR(
    deviceList: List<Device>,
    numCBR: Int,
    numTopics: Int,
    numMsgs: Int,
    intvl: Long
): MutableList<ConstBitRate> {
    assert(numCBR <= deviceList.size)
    assert(numCBR <= numTopics)

    val srcMsgSet = mutableSetOf<Int>()
    val topicSet = mutableSetOf<Int>()
    val cbrList = mutableListOf<ConstBitRate>()

    repeat(numCBR) {
        var srcIndex: Int
        do {
            srcIndex = MyRG.nextInt(deviceList.size)
        } while (srcMsgSet.contains(srcIndex))
        srcMsgSet.add(srcIndex)

        var topicId: Int
        do {
            topicId = MyRG.nextInt(numTopics)
        } while (topicSet.contains(topicId))
        topicSet.add(topicId)

        val startJitter = MyRG.nextLong(intvl)
        cbrList.add(ConstBitRate(srcIndex, deviceList[srcIndex].id, topicId, intvl, numMsgs, startJitter))
    }
    return cbrList
}

/**
 * A set of constant bit rate (CBR) traffic instances are deterministically generated.
 * This method maps source devices and topics using their indices to ensure reproducibility.
 *
 * @param deviceList The list of devices participating in the simulation.
 * @param numCBR The number of CBR traffic flows to generate.
 * @param numTopics The total number of available topics.
 * @param numMsgs The number of messages per CBR flow.
 * @param intvl The interval between message generations.
 * @return A list of deterministically generated CBR traffic flows.
 */
fun generateDeterministicCBR(
    deviceList: List<Device>,
    numCBR: Int,
    numTopics: Int,
    numMsgs: Int,
    intvl: Long
): MutableList<ConstBitRate> {
    val cbrList = mutableListOf<ConstBitRate>()

    repeat(numCBR) { i ->
        val srcId = deviceList[i].id
        val topicId = i % numTopics

        // Create CBR entry with a random start jitter within the interval
        val startJitter = MyRG.nextLong(intvl)
        val cbr = ConstBitRate(i, srcId, topicId, intvl, numMsgs, startJitter)

        cbrList.add(cbr)
    }

    return cbrList
}

/**
 * This sets cryptographic delay model to individual nodes.
 * @param nodesList A list of nodes including brokers and devices.
 * @param delayType A crypto delay model, e.g., CryptoDelayModel.CONST_CRYPTO_MODEL.
 */
fun setCryptoModel(nodesList: List<Node>, delayType: CryptoDelayModel) {
    nodesList.forEach {
        when (delayType) {
            CryptoDelayModel.CONST_CRYPTO_MODEL -> {
                it.cryptoModel = ConstantCryptoModel()
            }
            CryptoDelayModel.LINEAR_CRYPTO_MODEL -> {
                it.cryptoModel = LinearCryptoModel()
            }
        }
    }
}