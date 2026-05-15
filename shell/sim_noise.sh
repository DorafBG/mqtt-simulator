#!/bin/bash
# Sweep Target: $EPS_LEVEL (ln10, ln4, ln2)
JAVA_RUN="java -jar build/libs/MQTT_Base.jar"
CMD_FILE="cmd_noise.tmp"
rm -f $CMD_FILE

# Fixed Parameters
BROKERS=100; DEVICES=1000; CIRCUIT=3; PUBINTVL=60; DELTA=0.0001; MAX_SEED=10
METHODS=("Vuvuzela" "DPMQTT" "OptDPMQTT")
SWEEP_TARGETS=("ln10" "ln4" "ln2")

for seed in $(seq 1 $MAX_SEED); do
    for method in "${METHODS[@]}"; do
        for eps in "${SWEEP_TARGETS[@]}"; do
            echo "$JAVA_RUN NOISE999 $method $BROKERS $DEVICES $CIRCUIT $PUBINTVL $eps $DELTA $seed" >> $CMD_FILE
            echo "$JAVA_RUN NOISE100 $method $BROKERS $DEVICES $CIRCUIT $PUBINTVL $eps $DELTA $seed" >> $CMD_FILE
        done
    done
done

parallel --jobs 8 --progress < $CMD_FILE
rm $CMD_FILE
