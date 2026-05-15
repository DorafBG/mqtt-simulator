package jp.ac.tmu.sakailab.mqtt.routing

import jp.ac.tmu.sakailab.mqtt.*
import jp.ac.tmu.sakailab.mqtt.util.MyRG

/**
 * The IoT broker class with the A-MQTT protocol.
 *
 * 1. Common process:
 * On receiving an encapsulated message from a neighbor node, this broker first decrypts the received message.
 * To this end, the received shall be enqueued into the CPU queue [cpuQueue].
 * Should the queue is full, the message will be discarded, and decrypted otherwise.
 * When the fireDecCompleted is called back, this broker processes a message based on its role.
 * recvFromNIC => addCpuQueue => fireDispatchCpuJob => fireDecCompleted
 *
 * 2. Source brokers:
 * On receiving a published message from a publisher device, it identifies a set of subscriber's broker IDs
 * from the public bulletin board. An onion is created for individual destination brokers.
 * After generating an onion, the encapsulated messages are sent out toward the destination brokers.
 * fireDecCompleted => recvEncapMsgFromPublisher => addCpuQueue (hence after, AbstractNode takes care of)
 *
 * 3. Intermediate brokers:
 * On receiving an encapsulated message from a broker, it peels off one layer of onion and forwards it
 * to the next broker.
 * fireDecCompleted => forwardEncapMsgToBroker => addCpuQueue (hence after, AbstractNode takes care of)
 *
 * 4. Destination brokers:
 * On receiving an encapsulated message from a broker, it identifies a set of IDs of the devices who
 * are interested in a given topic within its neighbors (associated devices).
 * The message is encrypted by the subscriber's key and individually forward an encapsulated message
 * to devices by unicast.
 * fireDecCompleted => forwardEncapMsgToDevice => addCpuQueue  (hence after, AbstractNode takes care of)
 *
 * @author Kazuya Sakai, Ph.D.
 */
open class AMqttBroker(id: Int, var circuitLength: Int = 3) :
    AbstractNode(id, nodeType = Node.NODE_BROKER, numCpuCores = 1), Broker {

    // the variable for associate devices and connected brokers
    protected val associatedDevicesInternal: MutableMap<Int, Device> = sortedMapOf()
    protected val connectedBrokersInternal: MutableMap<Int, Broker> = sortedMapOf()

    // A getter implementation for associated devices.
    override val associatedDevices: Map<Int, Device>
        get() = this.associatedDevicesInternal

    // A getter implementation for associated device IDs.
    override val associatedDeviceIds: Set<Int>
        get() = associatedDevices.keys

    // A getter implementation for connected IoT brokers.
    override val connectedBrokers: Map<Int, Broker>
        get() = this.connectedBrokersInternal

    /** A given broker instance is connected to this broker. */
    override fun setConnectedBrokers(broker: Broker) {
        connectedBrokersInternal[broker.id] = broker
    }

    /** A given device is associated with this broker. */
    override fun setAssociatedDevice(device: Device) {
        associatedDevicesInternal[device.id] = device
    }

    /**
     * This function is called back when the decryption operation is completed.
     * 1. Source brokers: this calls the recvEncapMsgFromPublisher function to generate an onion.
     * 2. Intermediate brokers: this forwards the encapsulated message to the next brokers.
     * 3. Destination brokers: this calls the forwardEncapMsgToDevice function for delivery.
     * @param params A list of parameters
     */
    override fun fireDecCompleted(msg: Msg) {
        if (Config.isTrace) println("${getNodeName()} peeled off one layer $msg at ${Scheduler.time}")

        // This broker first checks the sender of msg is one of the associated devices
        if (associatedDevices.containsKey(msg.getSndrId())) {
            // msg is sent by a publisher, this broker is the source broker
            this.recvEncapMsgFromPublisher(msg = msg as EncapMsg)
        } else {
            // one layer of an onion is peeled off.
            val encapMsg = msg.getEncapMsg()
            if (encapMsg?.getMsgType() == EncapMsg.MSG_ONION) {
                // This broker is an intermediate broker
                forwardEncapMsgToBroker(encapMsg)
            } else {
                // This broker is the destination broker.
                forwardEncapMsgToDevice(encapMsg as PubMsg)
            }
        }
    }

    /**
     * This function is called when this broker is the source broker and receives
     * an encapsulated message from an associated device (a publisher).
     * An onion is generated and a set of encryption events is scheduled.
     * @param params An encapsulated message.
     */
    open fun recvEncapMsgFromPublisher(msg: EncapMsg) {
        val pubMsg = msg.getEncapMsg() as PubMsg
        val destDeviceIdSet = PublicBulletinBoard.getDestDeviceIds(pubMsg.topicId)
        val destBrokerIdSet = sortedSetOf<Int>()

        // add a path trace log
        MsgTracer.onForwarded(pubMsg, this.id, Scheduler.time)

        // A circuit is generated for individual subscribers.
        var needCopy = false
        for (dstDeviceId in destDeviceIdSet) {
            val destBrokerId = PublicBulletinBoard.getDestBrokerId(dstDeviceId)
            if (destBrokerIdSet.contains(destBrokerId)) continue
            destBrokerIdSet.add(destBrokerId)

            // the published message is copied, since this is a simulation
            var innerMsg = if (needCopy) pubMsg.cloneMsg() as PubMsg else pubMsg

            // Onion circuit construction
            val encapMsg = constructOnion(innerMsg, destBrokerId, this.circuitLength)

            // add a CPU job
            addCpuQueue(encapMsg as Msg)

            // add log
            if (needCopy) MsgTracer.inheritRecord(pubMsg, encapMsg.getMsgId())

            // a copy of a message needs to be generated for the next destination device
            needCopy = true
        }
    }

    /**
     * This function is for this broker to forward an encapsulated message to the next broker.
     * The message is added into the CPU queue for encryption.
     * @param encapMsg An encapsulated message.
     */
    open fun forwardEncapMsgToBroker(encapMsg: EncapMsg) {
        // add a path trace log
        MsgTracer.onForwarded(encapMsg,this.id, Scheduler.time)

        // add a CPU job
        addCpuQueue(encapMsg as Msg)

        if (Config.isTrace) println("Broker $id forwards an onion $encapMsg at ${Scheduler.time}")
    }

    /**
     * This function is for the destination broker to deliver a published message to subscribers.
     * This broker is last broker of a circuit and the message is forwarded to individual subscribers.
     * Here, unicast is used in order to make sure that the privacy is ensured.
     * @param pubMsg A published message.
     */
    open fun forwardEncapMsgToDevice(pubMsg: PubMsg) {
        // Find subscriber devices within the associated device sets.
        val destDeviceIdSet = getDestIdSetInAssociatedDevices(pubMsg.topicId)
        if (Config.isTrace) println("Broker $id start broadcasting a message to $destDeviceIdSet at ${Scheduler.time}")

        // add a path trace log
        MsgTracer.onForwarded(pubMsg,this.id, Scheduler.time)

        // construct an encapsulated message.
        var needCopy = false
        for (deviceId in destDeviceIdSet) {
            val innerMsg = if (needCopy) pubMsg.cloneMsg() as PubMsg else pubMsg

            // A message is encrypted by the subscriber's key, and encryption is performed.
            val encapMsg = Onion(this.id, deviceId, innerMsg as EncapMsg)
            encapMsg.setConstantMsgSize(1) // from a broker to a device, there will be always one encryption layer.

            // add a CPU job
            addCpuQueue(encapMsg as Msg)

            // add log
            if (needCopy) MsgTracer.inheritRecord(pubMsg, encapMsg.getMsgId())

            // a copy of a message needs to be generated for the next destination device
            needCopy = true
        }
    }

    /**
     * This function returns a set of IDs of devices who are interested in a given topic ID.
     * @param topicId A topic ID.
     * @return Returns a set of IDs of the devices which are subscribing a given topic.
     */
     protected open fun getDestIdSetInAssociatedDevices(topicId: Int): Set<Int> {
        return associatedDevices.values
            .filter { it.subscTopics.contains(topicId) }
            .map { it.id }
            .toSortedSet()
    }

    /**
     * This function constructs an onion circuit.
     * The circuit length includes the source and destination brokers.
     * Thus, the first onion router is this broker
     * The last broker is given by one of the arguments.
     * The intermediate brokers are randomly selected, which are distinct.
     * Note: when K = 3 (b_src -> b_relay -> b_dest), the number of encryption will be 2.
     * A message m is encrypted by b_dst's key, and then, by b_relay's key
     *
     * @param pubMsg A copy of a published message.
     * @param dstBrokerId The destination broker ID.
     * @param circuitLength The circuit length.
     * @return Returns a layered encapMsg instance.
     */
    protected open fun constructOnion(pubMsg: PubMsg, dstBrokerId: Int, circuitLength: Int): EncapMsg {
        // The circuit length must be greater than or equal to 1.
        assert(circuitLength > 0)

        // Onion circuit construction
        val idSet = sortedSetOf(this.id, dstBrokerId) // the broker IDs used as intermediate routers
        val brokerIds = connectedBrokers.keys.toList()
        var nextBrokerId = dstBrokerId

        var innerMsg = pubMsg as EncapMsg
        var k = 0
        while (k < circuitLength - 2) { // Note: the last layer will be computed at the out of this loop.
            // Intermediate brokers are randomly selected
            val prevBrokerId = MyRG.nextListElement(brokerIds) ?:
                throw IllegalStateException("ERROR (AMqttBroker.kt): The list of broker IDs is empty.")

            // the same broker cannot be used twice as intermediate onion routers
            if (idSet.contains(prevBrokerId)) continue
            idSet.add(prevBrokerId)

            val outerMsg = Onion(prevBrokerId, nextBrokerId, innerMsg)
            nextBrokerId = prevBrokerId
            innerMsg = outerMsg
            k++
        }

        // The last layer, in which this broker is the sender.
        val outerOnion = Onion(this.id, nextBrokerId, innerMsg)
        // Let the onion packet be of a constant length
        outerOnion.setConstantMsgSize(this.circuitLength)

        return outerOnion
    }

    /**`
     * This function computes the encryption delay of an encapsulated message,
     * which has multiple layers.
     * @param msg An encapsulated message with multiple layers.
     * @return Returns the encryption delay.
     */
     override fun getOnionEncDelay(msg: Msg): Long {
        val sizes: List<Int> = (msg as EncapMsg).getMsgSizeList()
        var sumDelay: Long = 0
        for (size in sizes.drop(1)) {
            // Note: the first element is the size of a PubMsg instance. We skip it.
            // This is because the first layer consists of a PubMsg instance + Oinon Header of the destination broker.
            sumDelay += cryptoModel.getEncDelay(size)
        }

        return sumDelay
    }

    override fun toString(): String = "Broker $id $associatedDeviceIds"
}