package jp.ac.tmu.sakailab.mqtt.routing

import jp.ac.tmu.sakailab.mqtt.Msg

/**
 * Implementation of an Onion-encrypted message for secure routing.
 * This class serves as both an encapsulated message and a network message.
 *
 * There are three types of transmission.
 * 1. Form a device (i.e., a publisher) to a broker.
 * 2. From a broker to broker.
 * 3. Form a broker to a device (i.e., a subscriber).
 *
 * All the transmissions are encapsulated by Onion.
 *
 * At the routing protocol level, any messages are treated as an Onion class or PubMsg class.
 * In the NIC layer, all the messages are treated using the Msg interface.
 *
 * @author Kazuya Sakai, Ph.D.
 */
class Onion(
    private val sndrId: Int,
    private val rcvrId: Int,
    private val encapMsg: EncapMsg
) : EncapMsg, Msg {

    companion object {
        /**
         * The Tor header 16-byte : 5-byte fixed header + 11-byte relay header
         * 5-byte fixed header : 4-byte circuit ID and 1-byte command fields.
         * 11-byte relay header : sender and receiver IDs.
         * In our simulations, we assume that only the legitimate sender and receiver can
         * obtain the sender and receiver information from the relay header.
         */
        const val ONION_HDR_LEN = 16
    }

    // Oracle / Simulation metadata
    private var isNoise: Boolean = false
    private var constantMsgSize: Int = -1

    /** @return The message IDS. */
    override fun getMsgId(): Int = this.encapMsg.getMsgId()

    /** @return The message type ID for ONION. */
    override fun getMsgType(): Int = EncapMsg.MSG_ONION

    /** @return The inner encapsulated message. */
    override fun getEncapMsg(): EncapMsg = encapMsg

    /** @return The sender's unique ID. */
    override fun getSndrId(): Int = sndrId

    /** @return The receiver's unique ID. */
    override fun getRcvrId(): Int = rcvrId

    /** @return True if this onion is a noise packet. */
    override fun isNoiseMsg(): Boolean = isNoise

    /** @return True if the inner message is a fake publish request. */
    override fun isFakePubMsg(): Boolean = encapMsg.isFakePubMsg()

    /** Marks this onion and its inner message as noise. */
    override fun setNoiseMsg() {
        this.isNoise = true
        this.encapMsg.setNoiseMsg()
    }

    /**
     * Sets a uniform message size to prevent traffic analysis based on packet length.
     * @param circuitLength The length of the routing circuit.
     */
    override fun setConstantMsgSize(circuitLength: Int) {
        // PUBMSG_LEN should be defined in PubMsg.kt
        // The constant message length is set to be circuitLength * ONION_HDR_LEN + PubMsg.PUBMSG_LEN
        this.constantMsgSize = circuitLength * ONION_HDR_LEN + getPubMsgSize()
        this.encapMsg.setConstantMsgSize(circuitLength)
    }

    /**
     * This computes the total message size including the MQTT header, Tor header, and payload.
     * If [constantMsgSize] is set, returns that value to ensure anonymity.
     * @return Returns the size of this onion packet.
     */
    override fun getMsgSize(): Int {
        return if (this.constantMsgSize <= 0) {
            // Need to cast encapMsg to Msg to access getMsgSize() as in Java
            ONION_HDR_LEN + (encapMsg as Msg).getMsgSize()
        } else {
            this.constantMsgSize
        }
    }

    /**
     * @return Returns the published message size inside of this encapsulated message
     */
    override fun getPubMsgSize(): Int {
        return encapMsg.getPubMsgSize()
    }

    /**
     * Computes a list of message sizes at each layer.
     * @return Returns a list of message sizes at each layer
     */
    override fun getMsgSizeList(): List<Int> {
        val innerSizes = encapMsg.getMsgSizeList()
        val currentSize = innerSizes.last() + ONION_HDR_LEN
        return (innerSizes) + currentSize
    }

    /**
     * Creates a deep copy of the message.
     * @return A cloned message.
     */
    override fun cloneMsg(): Msg {
        // An Onion packet will never be copied during onion forwarding
        throw NotImplementedError("Deep copy not implemented yet.")
    }

    override fun toString(): String {
        val prefix = if (isNoise) "Noise " else ""
        return "[$prefix$sndrId, $rcvrId, $encapMsg]"
    }
}