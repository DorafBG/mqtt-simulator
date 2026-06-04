package jp.ac.tmu.sakailab.mqtt.routing.jumpmqtt

import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.Event
import jp.ac.tmu.sakailab.mqtt.MsgTracer
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.routing.AMqttDevice
import jp.ac.tmu.sakailab.mqtt.routing.PublicBulletinBoard
import jp.ac.tmu.sakailab.mqtt.routing.UnicastPubMsg


/**
 * An implementation of Jump-Routing Device.
 *
 */
class JumpRoutingDevice(id: Int) : AMqttDevice(id) {
    /**
     * This function generates a new publication message and schedules an encryption event.
     * @param topicId A topic ID of a message to be published.
     * @param time  The timestamp that a published message is generated.
     */
    override fun genPubMsg(topicId: Int, time: Long) {
        // Retrieve set of destination device IDs for the given topic
        val destIdSet = PublicBulletinBoard.getDestDeviceIds(topicId)

        for (destId in destIdSet) {
            // Create a new Jump-Routing-specific publication message
            val msg = UnicastPubMsg(this.id, topicId, destId)
            MsgTracer.onCreated(msg, this.id, time)

            // Schedule the encryption event
            val params = arrayOf<Any?>(msg)
            val e = Event(time, this::fireEncPubMsg, params)
            Scheduler.addEvent(e)

            if (Config.isTrace) {
                println("Device $id scheduled a message genesration: $msg at ${Scheduler.time}")
            }
        }
    }
}
