#!/bin/bash
# Sweep Target: $DELTA (0.1 to 0.0001)
JAVA_RUN="java -jar build/libs/MQTT_Base.jar"
CMD_FILE="cmd_delta.tmp"
rm -f $CMD_FILE

BROKERS=100; DEVICES=1000; CIRCUIT=3; PUBINTVL=60; EPSILON="ln2"; MAX_SEED=10
METHODS=("Vuvuzela" "DPMQTT" "OptDPMQTT")
SWEEP_TARGETS=(0.1 0.01 0.001 0.0001)

for seed in $(seq 1 $MAX_SEED); do
    for method in "${METHODS[@]}"; do
        for delta in "${SWEEP_TARGETS[@]}"; do
              # 1. scenario, 2. protocol name, 3. the number of brokers, 4. the number of devices
              # 5. the circuit length, 6. publication interval, 7. epsilon, 8. delta, 9. seed
            echo "$JAVA_RUN DELTA $method $BROKERS $DEVICES $CIRCUIT $PUBINTVL $EPSILON $delta $seed" >> $CMD_FILE
        done
    done
    echo "$JAVA_RUN DELTA AMQTT $BROKERS $DEVICES $CIRCUIT $PUBINTVL $EPSILON 0.0001 $seed" >> $CMD_FILE
done

parallel --jobs 8 --progress < $CMD_FILE
rm $CMD_FILE