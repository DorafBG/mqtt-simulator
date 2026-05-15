package jp.ac.tmu.sakailab.mqtt.routing.dpmqtt

import jp.ac.tmu.sakailab.mqtt.Broker
import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.ConstBitRate
import jp.ac.tmu.sakailab.mqtt.Event
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.util.MyRG

/*
 * Noise generator for DP-MQTT.
 *
 * @author Kazuya Sakai, Ph.D.
 */
object NoiseMsgGenerator: AbstractNoiseGenerator() {

    /**
     * Schedules fake PubMsg generation events.
     *
     * @param intvl The message publication interval.
     * @param numRounds The number of rounds of a simulations.
     * @param deviceList A list of devices.
     * @param brokerList A list of brokers.
     */
    fun generateFakePubMsgs(intvl: Long, numRounds: Int, deviceList: List<Any>, cbrList: List<Any>): Int {
        var numNoises: Int = 0 // The number of generated fake published messages.
        for (device in deviceList.filterIsInstance<DPMqttDevice>()) {
            val deviceId = device.id

            // Replaced Java's manual flag loop with Kotlin's 'any' for efficiency [cite: 2026-04-12]
            val hasCbrTraffic = (cbrList as List<ConstBitRate>).any { it.srcId == deviceId }
            if (hasCbrTraffic) continue

            var time = MyRG.nextLong(intvl)
            for (i in 0 until numRounds) {
                val params = arrayOf<Any?>(-1)
                val e = Event(
                    time,
                    device::fireGenFakePubMsg,
                    params
                )
                Scheduler.addEvent(e)

                if (Config.isTrace) {
                    println("Device $deviceId scheduled fake PubMsg generation events at $time")
                }

                time += intvl
                numNoises++
            }
        }

        return numNoises
    }
}