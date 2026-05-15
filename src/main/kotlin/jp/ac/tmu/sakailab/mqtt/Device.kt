package jp.ac.tmu.sakailab.mqtt

import jp.ac.tmu.sakailab.mqtt.routing.PubMsg

/**
 * Interface representing an IoT Device (Publisher/Subscriber).
 * @author Kazuya Sakai, Ph.D.
 */
interface Device {
    val id: Int

    /** The broker this device is currently associated with. */
    var associatedBroker: Broker?

    /** The ID of the associated broker. */
    val associatedBrokerId: Int

    /** Topics this device has subscribed to. */
    val subscTopics: Set<Int>

    /** List of messages successfully received by this device. */
    val recvPubMsgs: List<PubMsg>

    /** Subscribes to a new topic. */
    fun setSubscTopic(topicId: Int)

    /** Handles the reception of an encapsulated (often Onion) message. */
//    fun recvPubMsg(msg: EncapMsg)

    /** Generates and schedules a new publication message. */
    fun genPubMsg(topicId: Int, time: Long)

    /** Callback for event when a message is ready for encryption. */
    fun fireEncPubMsg(param: Array<Any?>)

    /** Callback for event when encryption is completed. */
//    fun fireEncCompleted(msg: Msg)

    /** Callback for event when decryption is completed. */
//    fun fireDecCompleted(msg: Msg)
}