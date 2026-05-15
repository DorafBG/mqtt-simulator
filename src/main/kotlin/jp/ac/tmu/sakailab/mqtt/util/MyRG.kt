package jp.ac.tmu.sakailab.mqtt.util

import kotlin.random.Random

/**
 * Singleton object for random number generation.
 * This class provides various probability distributions for network simulations.
 *
 * @author Kazuya Sakai, Ph.D.
 */
object MyRG {
    // 1. Private
    private var _rand: Random? = null

    // 2. Public access
    val rand: Random
        get() = _rand ?: throw IllegalStateException("MyRG must be initialized with a seed.")

    /**
     * Internal random engine. Throws [IllegalStateException] if accessed before [init].
     */
    fun init(seed: Int) {
        _rand = Random(seed)
    }

    /**
     * @param n The max value of an integer to be generated.
     * @return Returns an integer between 0 and n.
     */
    fun nextInt(n: Int) = _rand?.nextInt(n) ?: 0

    /**
     * @param n The max value of a long integer to be generated.
     * @return Returns a long integer between 0 and n.
     */
    fun nextLong(n: Long) = _rand?.nextLong(n) ?: 0

    /** @return A random integer from 0 (inclusive) to n (exclusive). */
    fun nextDouble() = _rand?.nextDouble() ?: 0.0

    /**
     * @param list A list of elements from which an element is selected at random.
     * @return Returns an element of a given list.
     */
    fun <T> nextListElement(list: List<T>): T? {
        // Returns null if the list is empty.
        if (list.isEmpty()) return null

        // if _rand is empty, returns the element of index 0
        val index = _rand?.nextInt(list.size) ?: 0
        return list[index]
    }

    /**
     * Generates a value from the exponential distribution.
     * @param lambda The rate parameter.
     * @return A random value based on the exponential distribution.
     */
    /**
     * Generates a value from N(mu, sigma^2).
     * @param mu The mean (expectation) of the distribution.
     * @param sigma The standard deviation.
     */
    fun getNormalDistribution(mu: Double, sigma: Double): Double {
        // If _rand is null, then the current timestamp will be used as a seed
        val seed = _rand?.nextLong() ?: System.currentTimeMillis()
        val engine = java.util.Random(seed)

        // N(0, 1) * sigma + mu
        return mu + (engine.nextGaussian() * sigma)
    }
}