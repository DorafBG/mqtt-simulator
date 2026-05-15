package jp.ac.tmu.sakailab.mqtt.radio

import jp.ac.tmu.sakailab.mqtt.Msg

/**
 * Interface representing a Network Interface Card (NIC) in the simulation.
 * Handles the bidirectional traffic between the physical layer and the upper routing layer.
 * @author Kazuya Sakai, Ph.D.
 */
interface NICIntf {
    companion object {
        const val NIC_TYPE_BROKER = 0x01 // the NIC for the IoT core
        const val NIC_TYPE_DEVICE = 0x02 // the NIC for IoT edges

        /**
         * The default transmission rates divided by 1000.0 to make them the second order
         * We do not write in the nanosecond order in order to compute TX delay more precisely,
         * which is left to NICIntfImpl.
         */
        const val TX_RATE_BROKER = 1.0 * 1024 * 1024 * 1024 // 1 Gbps
        const val TX_RATE_SENSOR = 256.0 * 1024.0 // 256 kbps
        const val TX_RATE_MOBILE = 256.0 * 1024 * 1024 // 256 Mbps
        const val TX_RATE_BACKBONE = 1.0 * 1024 * 1024 * 1024 * 1024 // 10 Gbps

        /**
         * Jitter is used to reduce a chance of collisions.
         * We follow NS2 mac-simple.cc, where jitter = rand * JITTER_MAX * 100-bit / TX rate
         * Here, JITTER_MAX is set to be 40 in NS2 mac-simple.cc
         */
        const val JITTER_CONST = 40
    }

    /**
     * Receives data from the Physical (Phy) layer.
     * @param params An array containing radio-level simulation parameters (e.g., signal strength, noise).
     *
     */
    fun recvFromPhy(params: Array<Any?>)

    /**
     * Receives a message from the upper layer (Routing/Application) to be transmitted.
     * @param m The network message to be sent.
     * @param delay The transmission delay in nanoseconds.
     *
     */
    fun recvFromUpperLayer(m: Msg, delay: Int)

    /**
     * Delivers a successfully received message to the upper layer.
     * @param m The network message received from the radio.
     *
     */
    fun sendToUpperLayer(m: Msg)

    /** @return Returns this NIC interface type name, such as broker, sensor, and mobile. */
    fun getNicTypeStr(): String
}