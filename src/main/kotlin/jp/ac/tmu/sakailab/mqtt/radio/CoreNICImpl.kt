package jp.ac.tmu.sakailab.mqtt.radio

import jp.ac.tmu.sakailab.mqtt.Node
import jp.ac.tmu.sakailab.mqtt.radio.NICIntf.Companion.NIC_TYPE_BROKER
import jp.ac.tmu.sakailab.mqtt.radio.NICIntf.Companion.TX_RATE_BROKER
import jp.ac.tmu.sakailab.mqtt.util.MyRG

/**
 * This is an NIC implementation class for brokers.
 *
 * IoT Core: Communications among brokers.
 * In the simulations, brokers are connected each other via NIC interfaces.
 * In reality, they are connected each other via internet.
 * We consider brokers are servers, e.g., Tor relays, and thus, the message forwarding
 * between two brokers may take ~200ms, since Tor relays are deployed to world wides.
 *
 * @author Kazuya Sakai, Ph.D.
 */
open class CoreNICImpl(node: Node, txRate: Double = TX_RATE_BROKER) :
    NICImpl(node, TX_RATE_BROKER, NIC_TYPE_BROKER) {
    companion object {
        const val NIC_TYPE_STR = "IoT-Core"
        const val TX_RATE_TOR_MIN = 1_000_000L // 1ms
        const val TX_RATE_TOR_MAX = 200_000_000L // 200ms
    }

    /** @return Returns the end-to-end delay, 50ms~200ms. */
    override fun getE2EDelay() : Long {
        return TX_RATE_TOR_MIN + MyRG.nextLong(TX_RATE_TOR_MAX - TX_RATE_TOR_MIN)
    }

    /** @return Returns this NIC interface type, such as broker, sensor, and mobile. */
    override fun getNicTypeStr(): String = NIC_TYPE_STR
}