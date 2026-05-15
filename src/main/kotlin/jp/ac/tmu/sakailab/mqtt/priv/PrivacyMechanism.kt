package jp.ac.tmu.sakailab.mqtt.priv

/**
 * Interface and Abstract implementation for Privacy Mechanisms (e.g., Differential Privacy).
 * Defines the privacy budget (epsilon, delta) and noise generation requirements.
 *
 * @author Kazuya Sakai, Ph.D.
 */
interface PrivacyMechanism {
    /** @return An array containing [epsilon, delta]. */
    fun getPrivacyBudget(): DoubleArray

    /** @return The number of noise messages to be injected. */
    fun getNumNoiseMsgs(): Int
}

/**
 * Abstract base class for privacy mechanisms.
 * Handles the parsing of epsilon values (e.g., "ln2", "0.1").
 * @param epsilonStr An epsilon with the string format.
 * @param delta A delta.
 */
abstract class AbstractPrivacyMechanism(
    epsilonStr: String,
    protected val delta: Double
) : PrivacyMechanism {

    /** Privacy budget epsilon. Parsed upon initialization. */
    protected val epsilon: Double = parseEpsilon(epsilonStr)

    /**
     * This function returns the privacy budget (epsilon, delta).
     * @return Returns teh privacy budget, (epsilon, delta).
     */
    override fun getPrivacyBudget(): DoubleArray {
        return doubleArrayOf(epsilon, delta)
    }

    /**
     * Parses a given string to a real number.
     * Supports standard double strings and "ln" prefixes (e.g., "ln2" -> 0.693...).
     ** @param A string such as "ln2", "0.1", etc.
     * @return Returns a double value of the epsilon.
     */
    protected fun parseEpsilon(s: String): Double {
        return when {
            s.startsWith("ln") -> {
                val value = s.substring(2).toDouble()
                // the base is e.
                kotlin.math.log(value, Math.E)
            }
            else -> s.toDouble()
        }
    }

    abstract override fun getNumNoiseMsgs(): Int
}