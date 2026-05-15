package jp.ac.tmu.sakailab.mqtt.routing.privmsg

/*
 * Pure Kotlin implementation of VuvuzelaNoiseMsgGenerator.
 * @author Kazuya Sakai, Ph.D.
 */

import jp.ac.tmu.sakailab.mqtt.util.MyRG
import jp.ac.tmu.sakailab.mqtt.ConstBitRate
import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.Device
import jp.ac.tmu.sakailab.mqtt.Event
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.routing.dpmqtt.AbstractNoiseGenerator

object VuvuzelaNoiseMsgGenerator: AbstractNoiseGenerator() {
    /**
     * This function generates and schedules noise generation events by calling the one in the abstract class.
     * Since the singleton pattern is applied for this object, a different function name is assigned
     * for noise generations to avoid confusion.
     *
     * @param intvl The message publication interval.
     * @param numRounds The number of rounds of a simulations.
     * @param brokerList A list of brokers.
     */
    fun generateVuvuzelaNoiseMsgs(intvl: Long, numRounds: Int, topicIdRange: Int, brokerList: List<Any>): Int {
        return super.generateNoiseMsgs(intvl, numRounds, topicIdRange, brokerList)
    }

    /**
     * Schedules fake VuvuzelaPubMsg generation events from non-CBR devices.
     *
     * @param intvl The interval of message publications.
     * @param numRounds The number of rounds.
     * @param deviceList A list of devices
     * @param cbrList A list of CBR traffic.
     */
    fun generateFakeVuvuzelaPubMsgs(intvl: Long, numRounds: Int, deviceList: List<Device>, cbrList: List<ConstBitRate>): Int {
        var numNoises: Int = 0; // The number of generated fake published messages.
        for (device in deviceList.filterIsInstance<VuvuzelaDevice>()) {
            // assertion
            if (device.associatedBrokerId < 0) {
                System.err.println("CRITICAL: Device $device.id has no associated broker! (ID: $device.associatedBrokerId)")
                System.exit(1)
            }

            // Check if this device generates CBR traffic to avoid overlapping
            val hasCbrTraffic = cbrList.any { it.srcId == device.id }
            if (hasCbrTraffic) continue

            var time = MyRG.nextLong(intvl)
            for (i in 0 until numRounds) {
                val params = arrayOf<Any?>(-1) // -1 indicates a fake message
                val e = Event(time, device::fireGenFakeVuvuzelaPubMsg, params)
                Scheduler.addEvent(e)

                if (Config.isTrace) {
                    println("Device ${device.id} scheduled fake ExtPubMsg generation events at $time")
                }

                time += intvl
                numNoises++
            }
        }

        return numNoises
    }
}