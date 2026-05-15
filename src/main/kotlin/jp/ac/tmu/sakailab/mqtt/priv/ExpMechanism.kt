package jp.ac.tmu.sakailab.mqtt.priv

import jp.ac.tmu.sakailab.mqtt.util.MyRG

/**
 * Implementation of the exponential mechanism for differential privacy.
 *
 * @author Kazuya Sakai, Ph.D.
 */
class ExpMechanism(
    epsilonStr: String,
    delta: Double,
    sensitivity: Double
) : AbstractPrivacyMechanism(epsilonStr, delta) {
    companion object {
        val NOISE_MAX: Int = 10000 // the maximal number of noises at one sample.
    }

    protected val lambda: Double
    protected val avg: Double

    init {
        // lambda = ln(delta - 1.0) / sensitivity
        // When delta - 1.0 is a negative value, then log returns NaN.
        if (this.delta <= 1.0) {
            println("delta must be greater than 1.0, $delta")
            System.exit(1)
        }
        this.lambda = kotlin.math.log(this.delta - 1.0, Math.E) / sensitivity
        this.avg = 1.0 / this.lambda
    }

    override fun getPrivacyBudget(): DoubleArray {
        return doubleArrayOf(this.epsilon, this.delta, this.lambda)
    }

    /**
     * This function has not been implemented yet.
     * @return Returns the number of noise messages to be generated.
     */
    override fun getNumNoiseMsgs(): Int {
        // 1. Set a probability simplex
        val probabilities = DoubleArray(NOISE_MAX + 1)
        var sum = 0.0

        // 2. compute exponential weight
        for (k in 0..NOISE_MAX) {
            // A utility function: u(k) = -k (having fewer noises is better)
            // probability = exp(epsilon * u(k) / (2 * sensitivity))
            val weight = kotlin.math.exp((epsilon * -k.toDouble()) / (2.0 * 1.0))
            probabilities[k] = weight
            sum += weight
        }

        // 3. sampling by cumulative probability function
        val r = MyRG.nextDouble() * sum
        var cumulativeSum = 0.0
        for (k in 0.. NOISE_MAX) {
            cumulativeSum += probabilities[k]
            if (r <= cumulativeSum) {
                return k
            }
        }
        return NOISE_MAX
    }

    override fun toString(): String {
        return "epsilon = $epsilon, delta = $delta, lambda = $lambda, avg = $avg"
    }
}