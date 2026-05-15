package jp.ac.tmu.sakailab.mqtt

import jp.ac.tmu.sakailab.mqtt.util.MyRG

/**
 * Represents a Constant Bit Rate (CBR) traffic model for an IoT device.
 * * @property srcIndex The index of the device (not ID) in the list.
 * @property srcId The ID of an IoT device.
 * @property topicId The topic ID.
 * @property intvl The message generation interval in milliseconds.
 * @property numMsgs The number of published messages to be generated.
 * @property initTime The timestamp when this CBR starts.
 *
 * @author Kazuya Sakai, Ph.D.
 */
class ConstBitRate(
    val srcIndex: Int,
    val srcId: Int,
    val topicId: Int,
    val intvl: Long,
    val numMsgs: Int = Int.MAX_VALUE, // Default to MAX_VALUE if not specified
    var initTime: Long = 0             // Initialized to 0 by default
) {
    /**
     * This function randomize [initTime] by jitter = this.intvl.
     */
    fun randomizeInitTime() {
        this.initTime = MyRG.nextLong(this.intvl)
    }

    override fun toString(): String {
        return "CBR $srcIndex $srcId $topicId $intvl"
    }
}