package jp.ac.tmu.sakailab.mqtt.routing

import jp.ac.tmu.sakailab.mqtt.*
import jp.ac.tmu.sakailab.mqtt.AbstractNode

/**
 * The IoT device class with the A-MQTT protocol.
 * IoT devices serve on either a publisher or subscribers of individual MQTT sessions.
 *
 * 1. Publishers:
 * On invoking a message publication event, it first encrypts a published message by its associated broker's key.
 * genPubMsg is called by the IoT generator in the simulation setup.s
 * fireEncPubMsg => addCpuQueue (hence after, AbstractNode takes cares of)
 *
 * 2. Subscribers:
 * On receiving an encapsulated message (which should be a PubMsg instance), it first decrypts
 * the encapsulated message. Then, it stores a received published message to its application buffer.
 * The message delivery of an MQTT session completes.
 * fireDecCompleted => the published messsage has been successfully received.
 *
 * @author Kazuya Sakai, Ph.D.
 */
open class AMqttDevice(id: Int) :
    AbstractNode(id, nodeType = Node.NODE_DEVICE, numCpuCores = 1), Device {

    // Device parameters
    override val associatedBrokerId: Int
        get() = associatedBroker?.id ?: -1
    override var associatedBroker: Broker? = null
    override var subscTopics: MutableSet<Int> = sortedSetOf()
        protected set

    // Application-layer storage for noise messages
    override val recvPubMsgs: MutableList<PubMsg> = mutableListOf()

    /** Add subscribed topic ID */
    override fun setSubscTopic(topicId: Int) {
        subscTopics.add(topicId)
    }

    /**
     * This function is called in order to schedule message publication events.
     * Only publisher devices perform this function.
     * In general, all the message generation events are schedule at the beginning of a simulation.
     * Thus, an encryption event of a published message is scheduled at the end of this function.
     *
     * @param topicID A topic ID of a generated published message.
     * @param time the global timestamp for a published message is generated.
     */
    override fun genPubMsg(topicId: Int, time: Long) {
        // a published message
        val msg = PubMsg(this.id, topicId)
        MsgTracer.onCreated(msg, this.id, time)

        // generate a message publication event
        val e = Event(time, this::fireEncPubMsg, arrayOf<Any?>(msg))
        Scheduler.addEvent(e)

        if (Config.isTrace) {
            println("${getNodeName()} scheduled a message generation, $msg at ${time}")
        }
    }

    /**
     * This function is called back when an encryption event starts.
     * Only publisher devices perform this function.
     * First, a published message is encrypted by the associated broker's key.
     * Then, the fireEncCompleted event is scheduled.
     *
     * @param msg A published message to be generated.
     */
    override fun fireEncPubMsg(param: Array<Any?>) {
        assert(param.size == 1)
        val msg = param[0] as PubMsg

        // A published message is encrypted
        val encapMsg = Onion(this.id, associatedBrokerId, msg as EncapMsg)
        encapMsg.setConstantMsgSize(1) // set the constant message size with one encryption layer

        // add a CPU job
        addCpuQueue(encapMsg)
    }

    /**
     * This function is called back when the decryption event is completed.
     * Only subscriber devices perform this function.
     * The published message is stored in the application buffer.
     *
     * @param msg An encapsulated received message.
     */
    override fun fireDecCompleted(msg: Msg) {
        val pubMsg = msg.getEncapMsg() as PubMsg

        this.recvPubMsgs.add(pubMsg)
        MsgTracer.onDelivered(pubMsg, this.id, Scheduler.time)

        if (Config.isTrace) {
            println("${getNodeName()} decrypted a message, $pubMsg at ${Scheduler.time}")
        }
    }

    /**`
     * Any device performs only one encryption in MQTT.
     * Thus, the delay for multi-layer encryption is simply the encryption delay.
     * @param msg A published message.
     * @return Returns the encryption delay.
     */
    override fun getOnionEncDelay(msg: Msg): Long {
        return cryptoModel.getEncDelay((msg as PubMsg).getMsgSize())
    }

    override fun toString(): String {
        return "Device $id ${associatedBrokerId}, $subscTopics"
    }
}