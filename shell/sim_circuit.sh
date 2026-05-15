#!/bin/bash
# Sweep Target: $CIRCUIT (3, 5, 7, 9)
JAVA_RUN="java -jar build/libs/MQTT_Base.jar"
CMD_FILE="cmd_circuit_flat.tmp"
rm -f $CMD_FILE

# Fixed Parameters
BROKERS=100; DEVICES=1000; PUBINTVL=60; DELTA=0.0001; MAX_SEED=10
METHODS=("Vuvuzela" "DPMQTT" "OptDPMQTT")
CIRCUITS=(3 5 7 9)

for circuit in "${CIRCUITS[@]}"; do
    for seed in $(seq 1 $MAX_SEED); do
        for method in "${METHODS[@]}"; do
              # 1. scenario, 2. protocol name, 3. the number of brokers, 4. the number of devices
              # 5. the circuit length, 6. publication interval, 7. epsilon, 8. delta, 9. seed
            echo "$JAVA_RUN CIR $method $BROKERS $DEVICES $circuit $PUBINTVL ln10 $DELTA $seed" >> $CMD_FILE
            echo "$JAVA_RUN CIR $method $BROKERS $DEVICES $circuit $PUBINTVL ln4 $DELTA $seed" >> $CMD_FILE
            echo "$JAVA_RUN CIR $method $BROKERS $DEVICES $circuit $PUBINTVL ln2 $DELTA $seed" >> $CMD_FILE

        done
        echo "$JAVA_RUN CIR AMQTT $BROKERS $DEVICES $circuit $PUBINTVL 1 $DELTA $seed" >> $CMD_FILE
    done
done

parallel --jobs 8 --progress < $CMD_FILE
rm $CMD_FILE