package jp.ac.tmu.sakailab.mqtt

import jp.ac.tmu.sakailab.mqtt.crypto.CryptoModel
import jp.ac.tmu.sakailab.mqtt.radio.NICIntf
import kotlin.collections.MutableList

/**
 * Interface representing a network node (Broker or Device).
 * Defines the core capabilities for neighborhood management and NIC interaction.
 * @author Kazuya Sakai, Ph.D.
 */
interface Node {

    companion object {
        const val NODE_BROKER: Int = 0x01
        const val NODE_DEVICE: Int = 0x02

        /** This is the broadcast address. */
        const val ADDR_BROADCAST = -127
    }

    val id: Int
    val nodeType: Int
    val nbr: Set<Node>
    var txQueueSize: Int
    var cpuQueueSize: Int
    var nic: NICIntf?
    var cryptoModel: CryptoModel
    val observers: MutableList<MqttObserver>

    /** Adds a new node to the neighbor set. */
    fun addNbr(node: Node)

    /** @return A set of unique IDs of the neighbors. */
    fun getNbrIds(): Set<Int>

    /** Checks if a specific node is within the neighbor set. */
    fun hasNodeInNbr(nodeId: Int): Boolean

    /** Receives a message from the NIC layer. */
    fun recvFromNIC(msg: Msg)

    /** Fires the event that this node completes a message transmission. */
    fun fireTxCompleted(params: Array<Any?>)

    /** Fires the event that this node starts transmitting a message. */
    fun fireTx()

    /** Clears all statistical variables for a new simulation run. */
    fun clear()

    /** returns an NIC instance in a type-safe way. */
    fun getSafeNic(): NICIntf {
        return this.nic ?: run {
            System.err.println("CRITICAL: No NIC has been bound to node ${this.id}")
            // Throw an exception
            throw IllegalStateException("NIC is missing for node ${this.id}")
        }
    }
}

/**
 * This is a sealed interface for CPU jobs such as encryption and decryption.
 */
sealed interface CPUJob {
    val msg: Msg

    /**
     * Encryption.
     * @param msg A message to be encrypted.
     */
    data class Encryption(override val msg: Msg) : CPUJob
    /**
     * Onion encryption.
     * @param msg An onion to be encrypted.
     */
    data class OnionEncryption(override val msg: Msg) : CPUJob
    /**
     * Decryption.
     * @param msg A message to be decrypted.
     */
    data class Decryption(override val msg: Msg) : CPUJob
}