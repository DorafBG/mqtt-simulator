package jp.ac.tmu.sakailab.mqtt

import jp.ac.tmu.sakailab.mqtt.routing.EncapMsg

/**
 * Interface for general messages in the network simulation.
 * Includes standard header sizes and basic message properties.
 * @author Kazuya Sakai
 */
interface Msg {

    companion object {
        /**
         * The standard MQTT header consists of fixed and variable fields.
         * The fixed field is of 2-byte length that includes essential information to control MQTT.
         * Additional information such as a publisher ID, a topic ID, and so on, must be added to the variable filed.
         */
        const val SIZE_MQTT_HDR = 2 // MQTT header size.

        // Header sizes in bytes
        const val SIZE_TCP_HDR = 20 // TCP header size.
        const val SIZE_IP_HDR = 20 // IP header size.
        const val SIZE_MAC_HDR = 34 // IEEE 802.x MAC frame header

        /**
         * The header size under the transport layer.
         * This should be added to onion packets before sending to the NIC.
         */
        const val SIZE_MSG_HDR = SIZE_TCP_HDR + SIZE_IP_HDR
    }

    /** @return The message IDS. */
    fun getMsgId(): Int

    /** @return The sender ID. */
    fun getSndrId(): Int

    /** @return The receiver ID. */
    fun getRcvrId(): Int

    /** @return The encapsulated message content. */
    fun getEncapMsg(): EncapMsg?

    /** @return The total size of the message including headers. */
    fun getMsgSize(): Int

    /** @return A deep copy of this message. */
    fun cloneMsg(): Msg
}