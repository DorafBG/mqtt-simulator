package jp.ac.tmu.sakailab.mqtt.routing.dpmqtt

import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.SimEvent
import jp.ac.tmu.sakailab.mqtt.priv.PrivacyMechanism
import jp.ac.tmu.sakailab.mqtt.routing.AMqttBroker
import jp.ac.tmu.sakailab.mqtt.routing.EncapMsg

/**
 * This is an abstract class for privacy enabled brokers.
 *
 * @author Kazuya Sakai, Ph.D.
 */
abstract class AbstractPrivacyEnabledBroker(id: Int, circuitLength: Int) : AMqttBroker(id, circuitLength) {
    // a privacy mechanism
    var privacyMech: PrivacyMechanism? = null

    /**
     * This generates noise messages based on a privacy mechanism.
     * @param A list of parameters, i.e., [[topicID]].
     */
    abstract fun fireGenNoiseMsgs(params: Array<Any?>)

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