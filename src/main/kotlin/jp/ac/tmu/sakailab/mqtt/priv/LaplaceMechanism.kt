package jp.ac.tmu.sakailab.mqtt.priv

import jp.ac.tmu.sakailab.mqtt.util.MyRG
import kotlin.math.log
import kotlin.math.max

/**
 * Implementation of the Laplace Mechanism for Differential Privacy.
 * Generates noise according to the Laplace distribution based on sensitivity, epsilon, and delta.
 * @author Kazuya Sakai, Ph.D.
 */
class LaplaceMechanism(
    epsilonStr: String,
    delta: Double,
    sensitivity: Double
) : AbstractPrivacyMechanism(epsilonStr, delta) {

    protected val mu: Double   // The mean (location parameter)
    protected val beta: Double // The scaling factor

    init {
        // Sensitivity / epsilon = beta
        this.beta = sensitivity / this.epsilon
        // mu = sensitivity - beta * ln(2 * delta)
        this.mu = sensitivity - this.beta * log(2.0 * this.delta, Math.E)
    }

    /**
     * @return An array containing [epsilon, delta, mu, beta].
     */
    override fun getPrivacyBudget(): DoubleArray {
        return doubleArrayOf(this.epsilon, this.delta, this.mu, this.beta)
    }

    /**
     * Calculates the number of noise messages using the Laplace distribution.
     * If epsilon is 0 or less, no noise is generated.
     * @return A non-negative integer representing the noise count.
     */
    override fun getNumNoiseMsgs(): Int {
        if (this.epsilon <= 0.0) return 0

        val x = nextLap()
        // Ensure the noise count is at least 0
        return max(0.0, x).toInt()
    }

    /**
     * Generates a random variable from the Laplace distribution using Inverse Transform Sampling.
     * Based on the CDF: F(x) = 1/2 * exp((x - mu) / beta) for x <= mu.
     *
     */
    protected fun nextLap(): Double {
        // If MyRG returns null, then r is set to be 0.0
        val r = MyRG.nextDouble() // Get a uniform random variable (0, 1]

        return if (r <= 0.5) {
            this.mu + this.beta * kotlin.math.log(2.0 * r, Math.E)
        } else {
            this.mu - this.beta * kotlin.math.log(2.0 * (1.0 - r), Math.E)
        }
    }

    override fun toString(): String {
        return "epsilon = $epsilon, delta = $delta, mu = $mu, beta = $beta"
    }
}