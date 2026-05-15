package jp.ac.tmu.sakailab.mqtt.routing.plainmqtt

import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.routing.AMqttDevice
import jp.ac.tmu.sakailab.mqtt.routing.EncapMsg
import jp.ac.tmu.sakailab.mqtt.routing.Onion
import jp.ac.tmu.sakailab.mqtt.routing.PubMsg

/**
 * An implementation of Plain MQTT device.
 * The published message is encrypted, but not a circuit is constructed.
 *
 * @author Kazuya Sakai, Ph.D.
 */
class PlainMqttDevice(id: Int) : AMqttDevice(id) {

    /**
     * This function is called when a message publication event is fired.
     * The difference form A-MQTT is that no constant message length is applied.
     * @param param A published message instance.
     */
    override fun fireEncPubMsg(param: Array<Any?>) {
        assert(param.size == 1)
        val innerMsg = param[0] as PubMsg // Note: the message will not be of constant length.

        // Encapsulate the message in an Onion layer for the associated broker.
        // The constant message length does not apply to Plain MQTT
        val encapMsg = Onion(this.id, this.associatedBrokerId, innerMsg as EncapMsg)

        // add a CPU job
        addCpuQueue(encapMsg as Msg)
    }
}