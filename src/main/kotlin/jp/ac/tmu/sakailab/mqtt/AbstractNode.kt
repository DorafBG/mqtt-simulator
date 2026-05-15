package jp.ac.tmu.sakailab.mqtt

import jp.ac.tmu.sakailab.mqtt.crypto.ConstantCryptoModel
import jp.ac.tmu.sakailab.mqtt.crypto.CryptoModel
import jp.ac.tmu.sakailab.mqtt.radio.NICIntf
import jp.ac.tmu.sakailab.mqtt.util.MyRG
import java.util.*

/**
 * This is an abstract node class that represents network nodes.
 * This provides basic functions and interfaces of encryption/decrpytion operations and NICs.
 * The number of CPU cores [numCpuCores] should be determined by inheritance classes.
 *
 * On receiving a message form the physical layer.
 * recvFromNIC: the received message is enqueued into the CPU queue.
 * => dispatchNextCpuJob (this function is repeated as long as there are available CPU cores.)
 * => 1. fireEncCompleted or 2. fireDecCompleted
 *    When the fireEncCompleted function is called, the encrypted message will be sent to the NIC interface.
 *    For the fireDecCompleted function, the detail action is left to inheritance classes,
 *    since it is node-type dependent, i.e. brokers and devices process a received message in a different way.
 *    In this abstract class, the CPU management and the Tx control at an NIC are defined.
 *
 * @author Kazuya Sakai, Ph.D.
 */
abstract class AbstractNode(
    override val id: Int,
    override val nodeType: Int,
    numCpuCores: Int = 1 // The number of CPU cores for cryptographic operations.
) : Node, Comparable<Node> {

    // Node parameters
    override val nbr: MutableSet<Node> = TreeSet() // an open neighbor set.
    override var txQueueSize: Int = Int.MAX_VALUE // the size of the message queue
    override var cpuQueueSize: Int = Int.MAX_VALUE // the CPU queue
    override var nic: NICIntf? = null // the NIC instance.
    override var cryptoModel: CryptoModel = ConstantCryptoModel() // Enc/dec operation delay model.
    override val observers: MutableList<MqttObserver> = mutableListOf(
        MsgObserver(),
        CryptoObserver()
    ) // This is to record the control overhead at this node.

    /** An indicator if its NIC is busy or not */
    protected var isNicBusy: Boolean = false
    /** The TX message queue */
    protected val txQueue: Queue<Msg> = LinkedList()
    /** The CPU core's busy states */
    private val isCpuBusy = BooleanArray(numCpuCores) { false }
    /** The CPU queue for cryptographic operations. */
    protected var cpuQueue: Queue<CPUJob> = LinkedList()

    /** @param node A node instance. */
    override fun addNbr(node: Node) {
        nbr.add(node)
    }

    /** Returns neighbor IDs. */
    override fun getNbrIds(): Set<Int> = nbr.map { it.id }.toSet()

    /** Checks for neighbor existence using idiomatic 'any'. */
    override fun hasNodeInNbr(nodeId: Int): Boolean = nbr.any { it.id == nodeId }

    /**
     * This function handles when this node receives a message.
     * If this is a broker instance, the message is of EncapMsg instance, and thus,
     * this node processes the received message by the recvEncapMsg function.
     * If this is a device instance, the message comes from the associated broker,
     * and thus, the message must be a PubMsg instance.
     * @param msg A message.
     */
    override fun recvFromNIC(msg: Msg) {
        if (Config.isTrace) println("${getNodeName()} received a message $msg at ${Scheduler.time}.")

        // add the message to the CPU queue
        if (this.cpuQueue.size < this.cpuQueueSize) {
            this.cpuQueue.add(CPUJob.Decryption(msg))
            fireDispatchCpuJob()
        } else {
            // drop the received message
            if (Config.isTrace) println("${getNodeName()} dropped a received message $msg.")
        }
    }

    /**
     * Clear the message queue and NIC state.
     */
    override fun clear() {
        txQueue.clear()
        isNicBusy = false
    }

    /** Callback for completed transmissions. */
    override fun fireTxCompleted(params: Array<Any?>) {
        isNicBusy = false
        if (txQueue.isNotEmpty()) {
            val msg = txQueue.poll()
            isNicBusy = true

            /*
             * A message transmission event is recorded here.
             * Do not directly call notifyObserver(.), since an implementation class of this
             * abstract class may override logMsg(.) to distinguish legitimate and noise messages.
             */
            logMsg(msg)

            // the message is sent to the NIC
            nic?.recvFromUpperLayer(msg, 0)
        }
    }

    /**
     * This function checks if its NIC is available or not. If so, the message
     * at the top of the queue is pooled and sent out to its NIC.
     * Then, the NIC function, i.e., recvFromUpperLayer, is called.
     */
    override fun fireTx() {
        if (!isNicBusy && txQueue.isNotEmpty()) {
            val msg = txQueue.poll()
            isNicBusy = true

            // write a log
            logMsg(msg)

            // the message is sent to the NIC
            nic?.recvFromUpperLayer(msg, 0)
        }
    }

    /**
     * This function dispatches a CPU job (e.g., encryption and decryption).
     */
    protected fun fireDispatchCpuJob() {
        if (cpuQueue.isEmpty()) return // no job in the CPU queue.

        // the index of an available CPU core.
        val availableCoreIndex = isCpuBusy.indices.firstOrNull { !isCpuBusy[it] }

        // dispatching
        if (availableCoreIndex != null) {
            val job = cpuQueue.poll()
            isCpuBusy[availableCoreIndex] = true // change the core's state to busy

            when (job) {
                is CPUJob.Encryption -> {
                    val delay = cryptoModel.getEncDelay(job.msg.getMsgSize())
                    scheduleCpuEvent(delay, availableCoreIndex, job.msg, this::fireEncCompleted)

                    // log
                    notifyObservers(SimEvent.Crypto(CryptoType.ENC, delay))
                }
                is CPUJob.OnionEncryption -> {
                    val delay = getOnionEncDelay(job.msg)
                    scheduleCpuEvent(delay, availableCoreIndex, job.msg, this::fireEncCompleted)

                    // log
                    notifyObservers(SimEvent.Crypto(CryptoType.ENC, delay))
                }
                is CPUJob.Decryption -> {
                    val delay = cryptoModel.getDecDelay(job.msg.getMsgSize())
                    scheduleCpuEvent(delay, availableCoreIndex, job.msg, this::fireDecCompleted)

                    // log
                    notifyObservers(SimEvent.Crypto(CryptoType.DEC, delay))
                }
            }

            // Proceed the next job, since there may be available CPU cores.
            fireDispatchCpuJob()
        }
    }

    /**`
     * This function computes the encryption delay of an encapsulated message,
     * which has multiple layers.
     * @param msg An encapsulated message with multiple layers.
     * @return Returns the encryption delay.
     */
    protected abstract fun getOnionEncDelay(msg: Msg): Long

    /**
     *
     */
    protected fun scheduleCpuEvent(delay: Long, coreIndex: Int, msg: Msg, action: (Msg) -> Unit) {
        // the timestamp when the CPU job will complete
        val time = Scheduler.time + delay

        // Things to do when this event is fired by lambda expression
        val callback: (Array<Any?>) -> Unit = { params ->
            val m = params[0] as Msg
            action(m)                       // 1. Call back the fireEncCompleted or fireDecCompleted func.
            isCpuBusy[coreIndex] = false    // 2. Change the CPU core state to idle.
            fireDispatchCpuJob()            // 3. Dispatch the next job if exists.
        }

        // an CPU event
        val e = Event(time, callback, arrayOf<Any?>(msg))
        Scheduler.addEvent(e)
    }

    /**
     * This function is called back when the encryption operation is completed.
     * When this IoT broker is the source broker associated with a publisher (a device),
     * it forward the encapsulated message to the next brokers.
     * When this IoT broker is the destination broker, it forwards the encapsulated message
     * to subscribers within associated devices.
     * @param msg An encrypted message to be sent.
     */
     protected open fun fireEncCompleted(msg: Msg) {
        if (Config.isTrace) println("${getNodeName()} encrypted an onion $msg at ${Scheduler.time}")

        // add the message to the TX queue, and shuffle when needed
        if (addTxQueue(msg)) {
            shuffleTxQueue()
        }
        this.fireTx()
    }

    /**
     *
     */
    protected abstract fun fireDecCompleted(msg: Msg)

    /**
     * Note: this function is called from only a routing module for encryption,
     * since any decryption operation shall be invoked by the lower layer.
     */
    protected fun addCpuQueue(msg: Msg) {
        // add the message to the CPU queue
        if (this.cpuQueue.size < this.cpuQueueSize) {
            if (Config.isTrace) {
                println("${getNodeName()} enqueued a message $msg to the CPU queue at ${Scheduler.time}")
            }
            this.cpuQueue.add(CPUJob.Encryption(msg))
            this.fireDispatchCpuJob()
        } else {
            // drop the received message
            if (Config.isTrace) {
                println("${getNodeName()} dropped a message $msg due to the CPU queue overflow at ${Scheduler.time}.")
            }
        }
    }

    /**
     * This function adds a message to the message queue if the buffer is not full.
     * When the queue is already full, the incoming message is discarded.
     * @param msg An enqueued message.
     * @return Returns true when a message is enqueued, and false otherwise.
     */
    protected fun addTxQueue(msg: Msg): Boolean {
        return if (txQueue.size < txQueueSize) {
            txQueue.add(msg)
            true
        } else {
            // Drop message if queue is full
            // if (Config.isTrace) println("A message is dropped.: $msg")
            false
        }
    }

    /**
     * This method shuffles the message queue.
     * @return Returns true if the queue has more than two messages, i.e., the queue is shuffled, and false otherwise.
     */
    protected fun shuffleTxQueue(): Boolean {
        return if (txQueue.size > 1) {
            (txQueue as? MutableList<Msg>)?.shuffle(MyRG.rand)
            true
        } else false
    }

    /**
     * Observer registrations.
     * @param observer An observer instance.
     */
    protected fun addObserver(observer: MqttObserver) {
        observers.add(observer)
    }

    /**
     * The observer design pattern is applied for recording control overhead at individual nodes.
     * @param event A simulation event, e.g., enc/dec operations and message transmissions.
     */
    protected fun notifyObservers(event: SimEvent) {
        observers.forEach { it.onEvent(this.id, event) }
    }

    /**
     * This function calls the notifyObserver(.) function for recording a log.
     * Please override this function if you want to log both legitimate messages and noise messages.
     * To be specific, differentially private-based protocols must override this to distinguish
     * legitimate messages and noise messages.
     *
     * @param msg A message to be logged.
     */
    open fun logMsg(msg: Msg) {
        notifyObservers(SimEvent.TXMsg(Msg.SIZE_MSG_HDR + msg.getMsgSize()))
    }

    /**
     * Nodes can be sorted by their types, brokers or devices, as well as their ID.
     * @param other The node instance to be compared with this node.
     */
    override fun compareTo(other: Node): Int {
        return if (this.nodeType == other.nodeType) {
            this.id.compareTo(other.id)
        } else {
            this.nodeType.compareTo(other.nodeType)
        }
    }

    /**
     *
     * @return Returns the name of this node, e.g., Broker 1, Device 101, etc.
     */
    protected fun getNodeName(): String {
        return when (nodeType) {
            Node.NODE_BROKER -> "Broker $id"
            Node.NODE_DEVICE -> "Device $id"
            else -> {
                println("Error: Invalid node type $nodeType")
                "Node Not Found"
            }
        }
    }
}