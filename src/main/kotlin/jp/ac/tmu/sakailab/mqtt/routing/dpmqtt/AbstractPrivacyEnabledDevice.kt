package jp.ac.tmu.sakailab.mqtt.routing.dpmqtt

import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.SimEvent
import jp.ac.tmu.sakailab.mqtt.routing.AMqttDevice
import jp.ac.tmu.sakailab.mqtt.routing.EncapMsg
import jp.ac.tmu.sakailab.mqtt.routing.PubMsg

/**
 * This is an abstract class for privacy enabled devices.
 *
 * @author Kazuya Sakai, Ph.D.
 */
abstract class AbstractPrivacyEnabledDevice(id: Int): AMqttDevice(id) {
    /**
     * Application-layer storage for noise messages
     */
    val recvNoiseMsgs: MutableList<PubMsg> = mutableListOf()

    /**
     * This function first classifies if a given message is either legitimate or noise.
     * Then, the message is given to the corresponding log notifier.
     *
     * @param msg A message to be logged.
     */
    override fun logMsg(msg: Msg) {
        if (msg is EncapMsg && msg.isNoiseMsg()) {
            notifyObservers(SimEvent.TxNoise(Msg.SIZE_MSG_HDR + msg.getMsgSize()))
        } else {
            notifyObservers(SimEvent.TXMsg(Msg.SIZE_MSG_HDR + msg.getMsgSize()))
        }
    }
}