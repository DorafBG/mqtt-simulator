package jp.ac.tmu.sakailab.mqtt.sim

import jp.ac.tmu.sakailab.mqtt.Config
import jp.ac.tmu.sakailab.mqtt.CryptoObserver
import jp.ac.tmu.sakailab.mqtt.MsgObserver
import jp.ac.tmu.sakailab.mqtt.MsgTracer
import jp.ac.tmu.sakailab.mqtt.Scheduler
import jp.ac.tmu.sakailab.mqtt.routing.PublicBulletinBoard
import jp.ac.tmu.sakailab.mqtt.routing.dpmqtt.DPMqttDevice
import jp.ac.tmu.sakailab.mqtt.routing.privmsg.VuvuzelaDevice

/**
 * DP-MQTT simulator engine.
 * @author Kazuya Sakai, Ph.D.
 */
object DPSimulator : Scheduler() {

    fun start(): Long {
        val config = Config

        // 1. Initialization
        time = 0
        config.cbrList?.forEach { cbr ->
            repeat(cbr.numMsgs) { i ->
                if (Config.isTrace) {
                    println("${cbr} ${PublicBulletinBoard.destDeviceIdTable[cbr.topicId]}")
                }

                // Schedule message publication events.
                config.deviceList?.get(cbr.srcIndex)?.genPubMsg(
                    cbr.topicId,
                    cbr.initTime + i * cbr.intvl
                )

                // keep the total number of published messages received by subscribers.
                val numDest = PublicBulletinBoard.destDeviceIdTable[cbr.topicId]?.size ?: 0
                config.numInitMsg += numDest
            }
        }

        // 2. Running a simulation
        run()

        // 3. Record results
        config.deviceList?.forEach { d ->
            val recvMsgs = d.recvPubMsgs
            config.numDeliMsg += recvMsgs.size

            // The protocol-dependent process : the number of noise messages are computed.
            when (config.protName) {
                "DPMQTT", "OptDPMQTT" -> {
                    config.numDeliNoiseMsg += (d as? DPMqttDevice)?.recvNoiseMsgs?.size ?: 0
                }

                "Vuvuzela" -> {
                    config.numDeliNoiseMsg += (d as? VuvuzelaDevice)?.recvNoiseMsgs?.size ?: 0
                }
            }
        }

        // The delivery rate and the number of delivered messages.
        config.delivery = MsgTracer.getDeliveryRate()
        config.delay = MsgTracer.getAverageDelay()

        // computes the control overhead.
        config.nodesList.forEach { node ->
            node.observers.forEach { observer ->
                when (observer) {
                    is MsgObserver -> {
                        // The message overhead and amount of traffic.
                        config.numMsgTx += observer.getTotalMsgTx()
                        config.amntMsgTrfc += observer.getTotalMsgTraffic()
                        config.numNoiseTx += observer.getTotalNoiseTx()
                        config.amntNoiseTrfc += observer.getTotalNoiseTraffic()
                    }

                    is CryptoObserver -> {
                        // The number of encryption and decryption operations.
                        config.numEnc += observer.getTotalNumEnc()
                        config.numDec += observer.getTotalNumDec()
                        config.encDelay += observer.getTotalEncDelay()
                        config.decDelay += observer.getTotalDecDelay()
                    }
                }
            }
        }
        config.totalTx = config.numMsgTx + config.numNoiseTx
        config.totalAmntTrfc = config.amntMsgTrfc + config.amntNoiseTrfc
        config.encDelay /= (config.numDeliMsg + config.numDeliNoiseMsg)
        config.decDelay /= (config.numDeliMsg + config.numDeliNoiseMsg)

        // show the state of the message tracer if needed.
        if (config.isTrace) {
            MsgTracer.recordMap.forEach { (msgId, record) ->
                println(
                    "MsgID: $msgId | Delivered: ${record.isDelivered} " +
                            "| delay: ${MsgTracer.getDelay(msgId) / 100_000} " +
                            "| Path: ${record.path} " +
                            "| Timestamps: ${record.timestamps} "
                )
            }
        }

        return time
    }
}