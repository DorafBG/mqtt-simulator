package jp.ac.tmu.sakailab.mqtt.crypto

/**
 * @author Kazuya Sakai, Ph.D.
 */

/**
 * This is an interface of a cryptographic model that computes the encryption
 * and decryption processing delays.
 */
interface CryptoModel {
    /**
     * @param size The size of message of which encryption delay is computed.
     * @return Returns the encryption delay in the nanosecond order.
     */
    fun getEncDelay(size: Int): Long

    /**
     * @param size The size of message of which decryption delay is computed.
     * @return Returns the decryption delay in the nanosecond order.
     */
    fun getDecDelay(size: Int): Long
}

/**
 * Cryptographic operation delay models.
 */
enum class CryptoDelayModel { CONST_CRYPTO_MODEL, LINEAR_CRYPTO_MODEL }

/**
 * This is a constant cryptographic operation processing model.
 * RSA with a 512-bit key, for data up to 1k bytes:
 * Enc and Dec are assumed to take 12ms and 30ms, respectively.
 *
 * Reference: S. Y. Bonde and U. S. Bhadade, “Analysis of Encryption Algorithms (RSA SRNN and 2 key pair)
 * for Information Security,” in ICCUBEA, 2017, pp. 1–5.
 */
class ConstantCryptoModel : CryptoModel {
    /** @return Returns encryption delay */
    override fun getEncDelay(size: Int): Long = 12_000_000L // 12ms
    /** @return Returns decryption delay */
    override fun getDecDelay(size: Int): Long = 30_000_000L // 30ms
}

/**
 * This is a linear cryptographic model, in which encryption and decryption delays
 * are determined by the size of a message to be encrypted/decrypted.
 * Delay(size) = alpha + beta * size
 * Here, alpha is a constant delay and beta is the scaling factor w.r.t. the message size.
 */
class LinearCryptoModel(
    private val encFixed: Double = 6.0,
    private val decFixed: Double = 10.0,
    private val baseSize: Double = 1024.0
) : CryptoModel {
    private val encBeta = (12.0 - encFixed) / baseSize
    private val decBeta = (30.0 - decFixed) / baseSize

    override fun getEncDelay(size: Int): Long {
        val delayMs = encFixed + (encBeta * size)
        return (delayMs * 1_000_000).toLong()
    }

    override fun getDecDelay(size: Int): Long {
        val delayMs = decFixed + (decBeta * size)
        return (delayMs * 1_000_000).toLong()
    }
}
