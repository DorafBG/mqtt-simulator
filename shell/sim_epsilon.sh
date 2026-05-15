#!/bin/bash
# Sweep Target: $EPSILON (ln10 to 0.1)
JAVA_RUN="java -jar build/libs/MQTT_Base.jar"
CMD_FILE="cmd_eps_flat.tmp"
rm -f $CMD_FILE

BROKERS=100; DEVICES=1000; CIRCUIT=3; PUBINTVL=60; DELTA=0.0001; MAX_SEED=10
METHODS=("Vuvuzela" "DPMQTT" "OptDPMQTT")
LEVELS=("ln10" "ln4" "ln3" "ln2" "0.5" "0.3" "0.2" "0.1")

for eps in "${LEVELS[@]}"; do
    for seed in $(seq 1 $MAX_SEED); do
        for method in "${METHODS[@]}"; do
              # 1. scenario, 2. protocol name, 3. the number of brokers, 4. the number of devices
              # 5. the circuit length, 6. publication interval, 7. epsilon, 8. delta, 9. seed
            echo "$JAVA_RUN EPS $method $BROKERS $DEVICES $CIRCUIT $PUBINTVL $eps $DELTA $seed" >> $CMD_FILE
        done
    done
done

parallel --jobs 8 --progress < $CMD_FILE
rm $CMD_FILE