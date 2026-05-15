package jp.ac.tmu.sakailab.mqtt.radio

import jp.ac.tmu.sakailab.mqtt.Node
import jp.ac.tmu.sakailab.mqtt.radio.NICIntf.Companion.NIC_TYPE_DEVICE
import jp.ac.tmu.sakailab.mqtt.radio.NICIntf.Companion.TX_RATE_SENSOR
import jp.ac.tmu.sakailab.mqtt.util.MyRG

/**
 * LoRaWAN: the communication from a broker to a sensor device (from a sensor device to a broker).
 * The end-to-end delay will be 100 milliseconds
 * Reference: Understanding the Limits of LoRaWAN, IEEE Communications Magazine, 2019.
 *
 * @author Kazuya Sakai, Ph.D.
 */
open class SensorNICImpl(node: Node, txRate: Double = TX_RATE_SENSOR) :
    NICImpl(node, TX_RATE_SENSOR, NIC_TYPE_DEVICE) {
    companion object {
        const val NIC_TYPE_STR = "Sensor"
        const val TX_RATE_SENSOR_MIN = 50_000_000L // 10ms
        const val TX_RATE_SENSOR_MAX = 100_000_000L // 50ms
    }

    /** @return Returns the end-to-end delay, 50ms~100ms. */
    override fun getE2EDelay() : Long {
        return TX_RATE_SENSOR_MIN + MyRG.nextLong(TX_RATE_SENSOR_MAX - TX_RATE_SENSOR_MIN)
    }

    /** @return Returns this NIC interface type, such as broker, sensor, and mobile. */
    override fun getNicTypeStr(): String = NIC_TYPE_STR
}