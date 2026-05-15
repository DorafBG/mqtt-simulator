package jp.ac.tmu.sakailab.mqtt

import jp.ac.tmu.sakailab.mqtt.routing.EncapMsg

/**
 * This class is to record the routing path, timestamps, and delivery state of a message.
 * @author Kazuya Sakai, Ph.D.
 */
data class PacketRecord(
    /**
     * A list of nodes that a message travels.
     */
    val path: MutableList<Int> = mutableListOf(),

    /**
     * A list of timestamps at intermediate nodes.
     * The timestamps when a node sends a message to the NIC are recorded,
     * but not at the time that a message is sent from the NIC to radio.
     */
    val timestamps: MutableList<Long> = mutableListOf(),

    /**
     * True when a message is delivered.
     */
    var isDelivered: Boolean = false
)

object MsgTracer {
    // A message trace map, where message ID must be unique.
    val recordMap = mutableMapOf<Int, PacketRecord>()

    /**
     * This function should be called when a published message is generated at a device,
     * or when a noise message is generated at a broker/device.
     * Note: the instance of an encapsulated message is required in order to filter out noise messages.
     *
     * @param msg The message instance of which information will be recorded.
     * @param nodeId The ID of a node which forwards a message.
     * @param time The current timestamp.
     */
    fun onCreated(msg: EncapMsg, nodeId: Int, time: Long) {
        if (!msg.isNoiseMsg()) updateTrace(msg.getMsgId(), nodeId, time)
    }

    /**
     * This function should be called when the forwarding event.
     * Note: the instance of an encapsulated message is required in order to filter out noise messages.
     *
     * @param msg The message instance of which information will be recorded.
     * @param nodeId The ID of a node which forwards a message.
     * @param time The current timestamp.
     */
    fun onForwarded(msg: EncapMsg, nodeId: Int, time: Long) {
        if (!msg.isNoiseMsg()) updateTrace(msg.getMsgId(), nodeId, time)
    }

    /**
     * This function should be called when the destination device receives a published message.
     * In addition to path recording, the delivery state will be set to be true.
     * Note: the instance of an encapsulated message is required in order to filter out noise messages.
     *
     * @param msg The message instance of which information will be recorded.
     * @param nodeId The ID of a node which forwards a message.
     * @param time The current timestamp.
     */
    fun onDelivered(msg: EncapMsg, nodeId: Int, time: Long) {
        if (!msg.isNoiseMsg()) updateTrace(msg.getMsgId(), nodeId, time)
        recordMap[msg.getMsgId()]?.let {
            it.isDelivered = true
        }
    }

    /**
     * Records message generation, forwarding, delivered events.
     * @param msgId The ID of a message to be recorded.
     * @param nodeId The ID of a node which forwards a message.
     * @param time The current timestamp.
     */
    private fun updateTrace(msgId: Int, nodeId: Int, time: Long) {
        val record = recordMap.getOrPut(msgId) { PacketRecord() }
        record.path.add(nodeId)
        record.timestamps.add(time)
    }

    /**
     * This function shall be called when a copy of a message is generated.
     * Note: the instance of an encapsulated message is required in order to filter out noise messages.
     *
     * @param msg The original message instance to be copied.
     * @param copyId The ID of a message copy.
     */
    fun inheritRecord(msg: EncapMsg, copyId: Int) {
        if (!msg.isNoiseMsg()) {
            val original = recordMap[msg.getMsgId()] ?: return
            // prepend the path along which the original message has traveled.
            recordMap[copyId] = PacketRecord(
                path = original.path.toMutableList(),
                timestamps = original.timestamps.toMutableList()
            )
        }
    }

    /**
     * This function computes the end-to-end delay of the message with a given ID.
     * @param msgId A message ID.
     */
    fun getDelay(msgId: Int): Long {
        val times = recordMap[msgId]?.timestamps ?: return 0L
        if (times.size < 2) return 0L
        return times.last() - times.first()
    }

    fun getAverageDelay(): Double {
        // 1. filter out the records of undelivered messages
        val deliveredRecords = recordMap.values.filter { it.isDelivered }

        if (deliveredRecords.isEmpty()) return 0.0

        // 2. delay of each record
        val totalLatency = deliveredRecords.sumOf { record ->
            val times = record.timestamps
            if (times.size >= 2) times.last() - times.first() else 0L
        }

        // 3. compute the average delay
        return totalLatency.toDouble() / deliveredRecords.size
    }

    /**
     * This function computes the delivery rate of a set of MQTT sessions.
     */
    fun getDeliveryRate(): Double {
        if (recordMap.isEmpty()) return 0.0
        val deliveredCount = recordMap.values.count { it.isDelivered }
        return deliveredCount.toDouble() / recordMap.size
    }

    fun getNumDeliveredMsg(): Int {
        if (recordMap.isEmpty()) return 0
        else return recordMap.values.count { it.isDelivered }
    }
}