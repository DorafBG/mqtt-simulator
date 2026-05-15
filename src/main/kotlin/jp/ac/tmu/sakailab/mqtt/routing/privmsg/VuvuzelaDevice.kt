package jp.ac.tmu.sakailab.mqtt.routing.privmsg

import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.Event
import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.MsgTracer
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.routing.*
import jp.ac.tmu.sakailab.mqtt.routing.dpmqtt.AbstractPrivacyEnabledDevice
import jp.ac.tmu.sakailab.mqtt.util.MyRG

/*
 * The Vuvuzela IoT Device class.
 *
 * @author Kazuya Sakai, Ph.D.
 */
class VuvuzelaDevice(id: Int) : AbstractPrivacyEnabledDevice(id) {

    override fun genPubMsg(topicId: Int, time: Long) {
        // Vuvuzela duplicates a message for each subscriber
        val destIdSet = PublicBulletinBoard.getDestDeviceIds(topicId)

        destIdSet.forEachIndexed { index, destId ->
            val msg = UnicastPubMsg(this.id, topicId, destId)
            MsgTracer.onCreated(msg, this.id, time)

            // Schedule encryption message event
            val eventParams = arrayOf<Any?>(msg)
            val e = Event(time, this::fireEncPubMsg, eventParams)
            Scheduler.addEvent(e)

            if (Config.isTrace) {
                println("Device $id scheduled a message generation $msg of copy $index at ${Scheduler.time}")
            }
        }
    }

    override fun fireDecCompleted(msg: Msg) {
        val pubMsg = (msg as EncapMsg).getEncapMsg() as UnicastPubMsg

        /**
         * Since a copy of a legitimate message had been copied at the destination broker toward multiple devices,
         * the message instance whose destination subscriber is not this device should not be recorded.
         */
        if (pubMsg.destId == this.id) MsgTracer.onDelivered(pubMsg, this.id, Scheduler.time)

        // Vuvuzela logic: messages not meant for this device are treated as noise
        if (pubMsg.isNoiseMsg() || pubMsg.destId != this.id) {
            this.recvNoiseMsgs.add(pubMsg)
        } else {
            this.recvPubMsgs.add(pubMsg)
        }

        if (Config.isTrace) {
            println("${getNodeName()} decrypted a message, $pubMsg at ${Scheduler.time}")
        }
    }

    /**
     * Generates fake messages for a randomly selected subscribed topic.
     */
    fun fireGenFakeVuvuzelaPubMsg(params: Array<Any?>) {
        if (subscTopics.isEmpty()) return
        val fakeTopicId = params[0] as Int

        val topicId = MyRG.nextInt(subscTopics.size)
        val destIdSet = PublicBulletinBoard.getDestDeviceIds(topicId)

        for (destId in destIdSet) {
            val pubMsg = UnicastPubMsg(this.id, fakeTopicId, destId).apply {
                setNoiseMsg()
            }

            val encapMsg = Onion(this.id, this.associatedBrokerId, pubMsg).apply {
                setConstantMsgSize(1) // Single layer for publisher-to-broker tx
                setNoiseMsg()
            }

            // add a CPU job
            addCpuQueue(encapMsg as Msg)
        }
    }
}