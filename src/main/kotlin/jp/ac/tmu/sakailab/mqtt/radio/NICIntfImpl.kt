package jp.ac.tmu.sakailab.mqtt.radio

import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.Event
import jp.ac.tmu.sakailab.mqtt.Node
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.radio.NICIntf.Companion.JITTER_CONST
import jp.ac.tmu.sakailab.mqtt.radio.NICIntf.Companion.TX_RATE_BROKER
import jp.ac.tmu.sakailab.mqtt.radio.NICIntf.Companion.TX_RATE_MOBILE
import jp.ac.tmu.sakailab.mqtt.radio.NICIntf.Companion.TX_RATE_SENSOR
import jp.ac.tmu.sakailab.mqtt.util.MyRG
import kotlin.math.ceil

abstract class NICImpl protected constructor(
    protected val node: Node,
    var txRate: Double,
    val nicType: Int,
) : NICIntf {

    companion object {
        var isNicTrace = false

        /**
         * If two nodes are directly connected, the propagation delay should be added as the end-to-end delay.
         */
        val propDelay: Long = 100_1000L // 100 microseconds

        @JvmStatic
        fun bindCoreNICInterface(n: Node, txRate: Double = TX_RATE_BROKER) {
            val nic = CoreNICImpl(n, txRate,)
            n.nic = nic
        }
        @JvmStatic
        fun bindSensorNICInterface(n: Node, txRate: Double = TX_RATE_SENSOR) {
            val nic = SensorNICImpl(n, txRate)
            n.nic = nic
        }
        @JvmStatic
        fun bindMobileNICInterface(n: Node, txRate: Double = TX_RATE_MOBILE) {
            val nic = MobileNICImpl(n, txRate)
            n.nic = nic
        }
    }

    override fun recvFromPhy(params: Array<Any?>) {
        assert(params.size == 1)
        val m = params[0] as Msg

        if (isNicTrace) {
            println("NIC: Node ${node.id} receives a message from ${m.getSndrId()}")
        }
        sendToUpperLayer(m)
    }

    override fun recvFromUpperLayer(m: Msg, delay: Int) {
        val nodesList = Config.nodesList
        val nextId = m.getRcvrId()

        // jitter = rand % 40 * 100 / TX rate (in nanoseconds)
        val jitter = MyRG.nextInt(JITTER_CONST) * 100 * 1000_000_000L / txRate
        val txTime = getTxTime(m) + jitter.toLong() + delay

        if (nextId == Node.ADDR_BROADCAST) {
            // Broadcast
            for (nbr in node.nbr) {
                val params = arrayOf<Any?>(m)

                val e = Event(
                    Scheduler.time + txTime + propDelay,
                    nbr.getSafeNic()::recvFromPhy,
                    params
                )
                Scheduler.addEvent(e)
            }
        } else {
            // Unicast
            if (!node.hasNodeInNbr(nextId)) {
                System.err.println("NICImpl ERROR: Invalid target node ID ($nextId). Node ${node.id} does not have a neighbor $nextId.")
                System.exit(1)
            }

            // Add Tx completed event
            val params1 = arrayOf<Any?>(m)
            val e1 = Event(
                Scheduler.time + txTime,
                this.node::fireTxCompleted,
                params1
            )
            Scheduler.addEvent(e1)

            /**
             * Add a receiving message event at the receiver NIC.
             * If the receiver NIC's owner is a device, then the device's end-to-end delay shall be set.
             * Broker's NIC type = 0x1
             * Device's NIC type = 0x2
             */
            val params2 = arrayOf<Any?>(m)
            val recvNic = nodesList[nextId].getSafeNic()
            val e2eDelay = if (this.nicType >= (recvNic as NICImpl).nicType) getE2EDelay()
                else (recvNic).getE2EDelay()
            val e2 = Event(
                Scheduler.time + txTime + e2eDelay,
                recvNic::recvFromPhy,
                params2
            )
            Scheduler.addEvent(e2)

            if (isNicTrace) {
                println("NIC: Node ${node.id} sends out a message to $nextId")
            }
        }
    }

    /**
     * This sends a received message to the upper-layer interface, i.e., a node.
     * The recvFromNIC function is called at the AbstractNode instance that is bound with this NIC instance.
     * @param m A received message.
     */
    override fun sendToUpperLayer(m: Msg) {
        this.node.recvFromNIC(m)
    }

    /**
     * This function computes the transmission delay in the nanosecond order for a given message.
     * @param m A message to be sent.
     * @return Returns the transmission delay for sending out a message, m.
     */
    protected open fun getTxTime(m: Msg): Long {
        return ceil((m.getMsgSize() * 8).toLong() * 1000000000 / txRate).toLong()
    }

    /** @return Returns the end-to-end delay between two nodes. */
    abstract fun getE2EDelay(): Long

    /** @return Returns this NIC interface type, such as broker, sensor, and mobile. */
    abstract override fun getNicTypeStr(): String
}