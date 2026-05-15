package jp.ac.tmu.sakailab.mqtt

import kotlin.collections.set

/**
 * The observer design pattern to monitor control overhead at individual brokers and devices.
 * To be specific, the modules record message/noise transmissions and cryptographic operations.
 *
 * @athor Kazuya Sakai, Ph.D.
 */

/**
 * This is a simulation event class that defines three events as follows.
 * 1. message transmissions, 2. noise transmissions, and cryptographic operations.
 */
sealed class SimEvent {
    // The message transmission event (the message size)
    data class TXMsg(val size: Int) : SimEvent()
    // The noise message transmission event (the noise message size)
    data class TxNoise(val size: Int) : SimEvent()
    // Cryptographic operation events
    data class Crypto(val type: CryptoType, val delay: Long) : SimEvent()
}

enum class CryptoType { ENC, DEC }

/**
 * Observer interface
 */
interface MqttObserver {
    fun onEvent(nodeId: Int, event: SimEvent)
}

/**
 * The message observer class that records message and noise transmission events.
 */
class MsgObserver : MqttObserver {
    // Key = node ID
    private val numMsgTx = mutableMapOf<Int, Int>()
    private val amntMsgTraffic = mutableMapOf<Int, Int>()
    private val numNoiseTx = mutableMapOf<Int, Int>()
    private val amntNoiseTraffic = mutableMapOf<Int, Int>()

    /**
     * This function records the number of message transmissions and the amount of traffic
     * for boh legitimate and noise messages.
     * @param nodeId A node ID.
     * @param event A type of simulation events, e.g., a message TX or noise TX.
     */
    override fun onEvent(nodeId: Int, event: SimEvent) {
        when (event) {
            is SimEvent.TXMsg -> {
                numMsgTx[nodeId] = numMsgTx.getOrDefault(nodeId, 0) + 1
                amntMsgTraffic[nodeId] = amntMsgTraffic.getOrDefault(nodeId, 0) + event.size
            }
            is SimEvent.TxNoise -> {
                numNoiseTx[nodeId] = numNoiseTx.getOrDefault(nodeId, 0) + 1
                amntNoiseTraffic[nodeId] = amntNoiseTraffic.getOrDefault(nodeId, 0) + event.size
            }
            else -> {} // does nothing
        }
    }

    /** @return Returns the total number of message transmissions. */
    fun getTotalMsgTx(): Int = numMsgTx.values.sum()
    /** @return Returns the total amount of message traffic. */
    fun getTotalMsgTraffic(): Int = amntMsgTraffic.values.sum()
    /** @return Returns the total number of noise message transmissions. */
    fun getTotalNoiseTx(): Int = numNoiseTx.values.sum()
    /** @return Returns the total amount of noise message traffic. */
    fun getTotalNoiseTraffic(): Int = amntNoiseTraffic.values.sum()
}

/**
 * The message observer class that records cryptographic operation events.
 * Note: the modules in MqttObserver.kt monitors control overhead of individual nodes,
 * and thus, this class does not distinguish if enc/dec operations are performed on
 * legitimate messages or noise messages.
 */
class CryptoObserver : MqttObserver {
    private val numEnc = mutableMapOf<Int, Int>()
    private val encDelay = mutableMapOf<Int, Long>()
    private val numDec = mutableMapOf<Int, Int>()
    private val decDelay = mutableMapOf<Int, Long>()

    /**
     * This function records cryptographic events, such as encryption and decryption operations.
     * The number of encryption/decryption operations and the amount of time consumed
     * for encryption/decryption are recoreded.
     *
     * @param nodeId A node ID.
     * @param event A simulation event, e.g., encryption and decryption.
     */
    override fun onEvent(nodeId: Int, event: SimEvent) {
        when (event) {
            is SimEvent.Crypto -> {
                when (event.type) {
                    CryptoType.ENC -> {
                        numEnc[nodeId] = numEnc.getOrDefault(nodeId, 0) + 1
                        encDelay[nodeId] = encDelay.getOrDefault(nodeId, 0) + event.delay
                    }

                    CryptoType.DEC -> {
                        numDec[nodeId] = numDec.getOrDefault(nodeId, 0) + 1
                        decDelay[nodeId] = decDelay.getOrDefault(nodeId, 0) + event.delay
                    }
                }
            }
            else -> {} // does nothing
        }
    }

    /** @return Returns the total number of encryption operations. */
    fun getTotalNumEnc(): Int = numEnc.values.sum()
    /** @return Returns the total amount of time consumed for message encryption. */
    fun getTotalEncDelay(): Long = encDelay.values.sum()
    /** @return Returns the total number of decrpytion operations. */
    fun getTotalNumDec(): Int = numDec.values.sum()
    /** @return Returns the total amount of time consumed for message decryption. */
    fun getTotalDecDelay(): Long = decDelay.values.sum()
}