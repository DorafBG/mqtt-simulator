package jp.ac.tmu.sakailab.mqtt.routing.dpmqtt

import jp.ac.tmu.sakailab.mqtt.Broker
import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.Event
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.util.MyRG

abstract class AbstractNoiseGenerator {
    /**
     * This function generates and schedules noise generation events.
     *
     * @param intvl The message publication interval.
     * @param numRounds The number of rounds of a simulations.
     * @param brokerList A list of brokers.
     */
    fun generateNoiseMsgs(intvl: Long, numRounds: Int, topicIdRange: Int, brokerList: List<Any>): Int {
        var numNoises: Int = 0; // The number of generated fake published messages.
        for (broker in brokerList.filterIsInstance<AbstractPrivacyEnabledBroker>()) {
            var time = MyRG.nextLong(intvl)

            for (i in 0 until numRounds) {
                // add a noise message generation event
                val params = arrayOf<Any?>(topicIdRange)
                val e = Event(time,broker::fireGenNoiseMsgs, params)
                Scheduler.addEvent(e)

                if (Config.isTrace) {
                    println("Broker ${(broker as Broker).id} scheduled noise generation events at $time")
                }

                time += intvl
                numNoises++
            }
        }

        return numNoises
    }
}