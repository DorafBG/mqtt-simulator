package jp.ac.tmu.sakailab.mqtt.routing.jumpmqtt

import jp.ac.tmu.sakailab.mqtt.Msg
import jp.ac.tmu.sakailab.mqtt.routing.EncapMsg

/**
 * Encapsulated message for Jump routing.
 * Uses MSG_ONION for compatibility with AMqttBroker dispatch logic.
 */
class JumpEncapMsg(
    private val sndrId: Int,
    private val rcvrId: Int,
    private val header: JumpHeader,
    private val encapMsg: EncapMsg
) : EncapMsg, Msg {

    companion object {
        // Fixed header length in bytes for simulation.
        const val JUMP_HDR_LEN = 16
    }

    private var isNoise: Boolean = false
    private var constantMsgSize: Int = -1

    fun getHeader(): JumpHeader = header

    override fun getMsgId(): Int = encapMsg.getMsgId()
    override fun getMsgType(): Int = EncapMsg.MSG_ONION
    override fun getEncapMsg(): EncapMsg = encapMsg
    override fun getSndrId(): Int = sndrId
    override fun getRcvrId(): Int = rcvrId
    override fun isNoiseMsg(): Boolean = isNoise
    override fun isFakePubMsg(): Boolean = encapMsg.isFakePubMsg()

    override fun setNoiseMsg() {
        this.isNoise = true
        this.encapMsg.setNoiseMsg()
    }

    override fun setConstantMsgSize(circuitLength: Int) {
        this.constantMsgSize = circuitLength * JUMP_HDR_LEN + getPubMsgSize()
        this.encapMsg.setConstantMsgSize(circuitLength)
    }

    override fun getMsgSize(): Int {
        return if (this.constantMsgSize <= 0) {
            JUMP_HDR_LEN + (encapMsg as Msg).getMsgSize()
        } else {
            this.constantMsgSize
        }
    }

    override fun getPubMsgSize(): Int = encapMsg.getPubMsgSize()

    override fun getMsgSizeList(): List<Int> {
        val innerSizes = encapMsg.getMsgSizeList()
        val currentSize = innerSizes.last() + JUMP_HDR_LEN
        return innerSizes + currentSize
    }

    override fun cloneMsg(): Msg {
        throw NotImplementedError("Deep copy not implemented for JumpEncapMsg.")
    }

    override fun toString(): String {
        val prefix = if (isNoise) "Noise " else ""
        return "[$prefix$sndrId, $rcvrId, $header, $encapMsg]"
    }
}

