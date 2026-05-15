package jp.ac.tmu.sakailab.mqtt.routing

/**
 * Interface for encapsulated messages in the MQTT simulation.
 * Replaces static constants with a companion object for better organization.
 * @author Kazuya Sakai, Ph.D.
 */
interface EncapMsg {

    companion object {
        const val MSG_PUB = 0x01
        const val MSG_ONION = 0x02
    }

    /** @return The message IDS. */
    fun getMsgId(): Int

    /** @return The type of the message (e.g., PUB, ONION). */
    fun getMsgType(): Int

    /** @return The nested encapsulated message, if any. */
    fun getEncapMsg(): EncapMsg?

    /** @return True if the message is a noise packet. */
    fun isNoiseMsg(): Boolean

    /** @return True if the message is a fake publish request. */
    fun isFakePubMsg(): Boolean

    /** Marks this message as a noise message. */
    fun setNoiseMsg()

    /** Sets a constant message size based on the [circuitLength]. */
    fun setConstantMsgSize(circuitLength: Int)

    /** @return Returns this encapsulated message size. */
    fun getMsgSize(): Int

    /** @return Returns the published message size inside of this encapsulated message */
    fun getPubMsgSize(): Int

    /** @return Returns a list of message sizes at each layer */
    fun getMsgSizeList(): List<Int>
}