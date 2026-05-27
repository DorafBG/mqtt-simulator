package jp.ac.tmu.sakailab.mqtt.routing.jumpmqtt

/**
 * Header fields for Jump routing.
 * All fields are simulation metadata (no real cryptography).
 */
data class JumpHeader(
    val destBrokerId: Int,
    val prevHopId: Int,
    val jumpTargetId: Int,
    val hopCount: Int,
    val maxHops: Int,
    val proof: String
)

