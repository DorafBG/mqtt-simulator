package jp.ac.tmu.sakailab.mqtt.routing

import jp.ac.tmu.sakailab.mqtt.Msg

/**
 * UnicastPubMsg interface adds a destination ID to the standard PubMsg
 * to facilitate point-to-point communication within a unicast-based anonymous routing protocol.
 *
 * @author Kazuya Sakai, Ph.D.
 */
class UnicastPubMsg : PubMsg {

    // The destination device ID
    val destId: Int

    /**
     * Primary constructor for new messages.
     * @param srcId A source device ID.
     * @param topicId A topic ID.
     * @param destId A destination device ID.
     */
    constructor(srcId: Int, topicId: Int, destId: Int) : super(srcId, topicId) {
        this.destId = destId
    }

    /**
     * Secondary constructor used for generating copies (clones).
     * @param srcId A source device ID.
     * @param topicId A topic ID.
     * @param destId A destination device ID.
     * @param data The data to be published.
     */
    private constructor(srcId: Int, topicId: Int, destId: Int, data: String) : super(srcId, topicId, data) {
        this.destId = destId
    }

    /**
     * Creates a deep copy of this message to avoid instance sharing hazards.
     * @return Returns a cloned message instance.
     */
    override fun cloneMsg(): Msg {
        // Create a new instance with the same data and metadata
        return UnicastPubMsg(this.srcId, this.topicId, this.destId, this.data).apply {
            if(this@UnicastPubMsg.isNoiseMsg()) {
                this.setNoiseMsg()
            }
        }
    }

    override fun toString(): String {
        return "[$srcId, $topicId, $destId, $data]"
    }
}