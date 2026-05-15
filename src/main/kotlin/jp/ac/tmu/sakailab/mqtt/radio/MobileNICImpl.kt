package jp.ac.tmu.sakailab.mqtt.radio

import jp.ac.tmu.sakailab.mqtt.Node
import jp.ac.tmu.sakailab.mqtt.radio.NICIntf.Companion.NIC_TYPE_DEVICE
import jp.ac.tmu.sakailab.mqtt.radio.NICIntf.Companion.TX_RATE_MOBILE
import jp.ac.tmu.sakailab.mqtt.util.MyRG

/**
 * LTE: communication from a broker to a mobile device (from a mobile device to a broker).
 * The end-to-end delay will be 50 milliseconds
 * Reference: URLLC (Ultra-Reliable Low-Latency Communications)
 *
 * @author Kazuya Sakai, Ph.D.
 */
open class MobileNICImpl(node: Node, txRate: Double = TX_RATE_MOBILE) :
    NICImpl(node, TX_RATE_MOBILE, NIC_TYPE_DEVICE) {
    companion object {
        const val NIC_TYPE_STR = "Mobile"
        const val TX_RATE_MOBILE_MIN = 10_000_000L // 10ms
        const val TX_RATE_MOBILE_MAX = 50_000_000L // 50ms
    }

    /** @return Returns the end-to-end delay, 10ms~50ms. */
    override fun getE2EDelay() : Long {
        return TX_RATE_MOBILE_MIN + MyRG.nextLong(TX_RATE_MOBILE_MAX - TX_RATE_MOBILE_MIN)
    }

    /** @return Returns this NIC interface type, such as broker, sensor, and mobile. */
    override fun getNicTypeStr(): String = NIC_TYPE_STR
}