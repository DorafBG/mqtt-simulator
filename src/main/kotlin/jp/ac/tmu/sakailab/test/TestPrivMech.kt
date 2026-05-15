package jp.ac.tmu.sakailab.test

import jp.ac.tmu.sakailab.mqtt.priv.ExpMechanism
import jp.ac.tmu.sakailab.mqtt.priv.LaplaceMechanism
import jp.ac.tmu.sakailab.mqtt.util.MyRG
import java.io.File

fun main() {
    // a seed is given, set a random seed here.
    MyRG.init(42)

    val iterations = 1000
    val sensitivity = 1.0
    val delta = 0.01

    // 1. Laplace Mechanism
    val laplace = LaplaceMechanism("ln2", delta, sensitivity)
    val laplaceResults = (1..iterations).map { laplace.getNumNoiseMsgs() }

    // 2. Exponential Mechanism has not been implemented yet
    val exp = ExpMechanism("ln2", 1.1, sensitivity)
    val expResults = (1..iterations).map { exp.getNumNoiseMsgs() }

    // the results are stored in a csv file, and later on it can be visualized by a Python program.
    File("mechanism_results.csv").printWriter().use { out ->
        out.println("laplace,exponential")
        for (i in 0 until iterations) {
            out.println("${laplaceResults[i]},${expResults[i]}")
        }
    }

    println("Simulation completed. 1000 samples saved to mechanism_results.csv")
    println("Laplace Params: $laplace")
    println("Exp Params: $exp")
}