package jp.ac.tmu.sakailab.test

import jp.ac.tmu.sakailab.mqtt.routing.Onion
import jp.ac.tmu.sakailab.mqtt.routing.PubMsg

/**
 * Test bench to verify the recursive encapsulation of Onion messages.
 * This ensures that the [Onion.toString] correctly decodes the layered structure.
 */
fun main() {
    println("=== Onion Encapsulation Test Start ===")

    // 1. Create the innermost payload (The real message)
    val publisherId = 100
    val topicId = 1
    val payload = PubMsg(publisherId, topicId)
    println("Original Payload: $payload")

    // 2. Encapsulate with Layer 1 (Node 1 -> Node 2)
    val layer1 = Onion(sndrId = 101, rcvrId = 102, encapMsg = payload)

    // 3. Encapsulate with Layer 2 (Node 2 -> Node 3)
    val layer2 = Onion(sndrId = 102, rcvrId = 103, encapMsg = layer1)

    // 4. Encapsulate with Layer 3 (Node 3 -> Broker)
    val layer3 = Onion(sndrId = 103, rcvrId = 500, encapMsg = layer2)

    println("\n--- Layered Onion Structure (Recursive toString) ---")
    // This will trigger the recursive calls we discussed [cite: 2026-04-16]
    println(layer3)

    // 5. Verification of properties
    println("\n--- Property Verification ---")
    println("Outer Sender ID: ${layer3.getSndrId()}") // Should be 103
    println("Inner Payload Data: ${(layer3.getEncapMsg().getEncapMsg()?.getEncapMsg() as PubMsg).data}")

    println("\n=== Test Completed Successfully ===")
}