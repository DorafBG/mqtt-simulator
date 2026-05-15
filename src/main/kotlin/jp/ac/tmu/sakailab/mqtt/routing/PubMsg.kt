package jp.ac.tmu.sakailab.mqtt.routing

import jp.ac.tmu.sakailab.mqtt.Msg

/**
 * Represents the original message published by a publisher.
 * In the simulation, this is encapsulated by an [Onion] class for secure routing.
 *
 * PubMsg is used as the original message that the publisher wishes to publish.
 * Please note that when a device sends a message to a broker,
 * a PubMsg instance is encapsulated by an Onion class.
 *
 * @author Kazuya Sakai, Ph.D.
 */
open class PubMsg : EncapMsg, Msg {

    companion object {
        /** the source publisher ID is of 4-byte length. */
        const val SOURC_ID_LEN= 4

        /**
         * The UUID / GUID standard tells us the ID should be at least 16-byte long.
         * MQTT 5.0 Alies defines topic IDs are at least 2-byte long, but when a hash function
         * is applied for the topic ID, the length should be 16-bytes (i.e., 128-bit).
         */
        const val TOPIC_ID_LEN = 16 // the topic ID is of 16-byte length.

        /**
         * The variable header field of an MQTT packet is protocol-dependent.
         * In our simulations, it consists of a source publisher ID and a topic ID.
         */
        const val MQTT_VARIABLE_HDR = SOURC_ID_LEN + TOPIC_ID_LEN

        /**
         * In general, MQTT clients have two buffers, read and write buffers, each of which is of 128-byte.
         * Thus, the default data size is set to be 128.
         */
        open var defaultPayloadLength: Int = 128

        /** Global sequence for message IDs */
        protected var pubMsgIdSeq = 0
        protected var copyMsgIdSeq = 0
    }

    // Immutable properties
    private val msgId: Int
    val srcId: Int
    val topicId: Int
    val data: String

    // Oracle access: simulation metadata (Mutable properties)
    var isNoise: Boolean = false
    // The default data size = 128-byte. IP MTU is 1500-byte including the header.
    var payloadLength: Int = defaultPayloadLength

    /**
     * Primary constructor for generating a new message.
     */
    constructor(srcId: Int, topicId: Int) {
        this.msgId = pubMsgIdSeq++
        this.srcId = srcId
        this.topicId = topicId
        this.data = "$srcId-$topicId-$msgId"
    }

    /**
     * Internal constructor used for cloning.
     */
    constructor(srcId: Int, topicId: Int, data: String) {
        this.msgId = pubMsgIdSeq++
        this.srcId = srcId
        this.topicId = topicId
        copyMsgIdSeq++
        this.data = "$data-copy-$copyMsgIdSeq"
    }

    /** @return The message IDS. */
    override fun getMsgId(): Int = msgId

    /** @return Always [EncapMsg.MSG_PUB]. */
    override fun getMsgType(): Int = EncapMsg.MSG_PUB

    /** @return Null, as PubMsg is the innermost message. */
    override fun getEncapMsg(): EncapMsg? = null

    /** @return The publisher's ID. */
    override fun getSndrId(): Int = srcId

    /** @return -1 as the specific receiver is handled by the broker. */
    override fun getRcvrId(): Int = -1

    /** @return True if this is a noise message. */
    override fun isNoiseMsg(): Boolean = isNoise

    /** @return True if topicId < 0, indicating a fake request. */
    override fun isFakePubMsg(): Boolean = topicId < 0

    /** Marks this message as noise. */
    override fun setNoiseMsg() {
        this.isNoise = true
    }

    /** No-op for the innermost message. */
    override fun setConstantMsgSize(circuitLength: Int) {
        // does nothing
    }

    /**
     * In the default setting, the length of published messages will be as follows.
     * The fixed MQTT header 2-byte + The variable MQTT field (a publisher ID, a topic ID) + data 128-byte
     * @return Returns the message size of this published message instance.
     */
    override fun getMsgSize(): Int = Msg.SIZE_MQTT_HDR + this.payloadLength

    /**
     * This returns the message size.
     * @return Return the size of this publihsed message.
     */
    override fun getPubMsgSize(): Int {
        return getMsgSize()
    }

    /** @return A list of message size at each layer. */
    override fun getMsgSizeList(): List<Int> {
        // this is the most inner message, and thus, PUBMSG_LEN is returned.
        return listOf(getMsgSize())
    }

    /**
     * Creates a deep copy for simulation forks.
     */
    override fun cloneMsg(): Msg {
        return PubMsg(srcId, topicId, data).also {
            it.isNoise = this.isNoise
        }
    }

    override fun toString(): String {
        return "[$srcId, $topicId, $data]"
    }
}