#!/bin/bash
# Sweep Target: $PUBINTVL (10000 to 60000)
JAVA_RUN="java -jar build/libs/MQTT_Base.jar"
CMD_FILE="cmd_intvl.tmp"
rm -f $CMD_FILE

BROKERS=100; DEVICES=1000; CIRCUIT=3; DELTA=0.0001; MAX_SEED=10
METHODS=("Vuvuzela" "DPMQTT" "OptDPMQTT")
SWEEP_TARGETS=("10" "20" "30" "40" "50" "60")

for pubintvl in "${SWEEP_TARGETS[@]}"; do
    for seed in $(seq 1 $MAX_SEED); do
        for method in "${METHODS[@]}"; do
              # 1. scenario, 2. protocol name, 3. the number of brokers, 4. the number of devices
              # 5. the circuit length, 6. publication interval, 7. epsilon, 8. delta, 9. seed
            echo "$JAVA_RUN INTVL $method $BROKERS $DEVICES $CIRCUIT $pubintvl ln10 $DELTA $seed" >> $CMD_FILE
            echo "$JAVA_RUN INTVL $method $BROKERS $DEVICES $CIRCUIT $pubintvl ln4 $DELTA $seed" >> $CMD_FILE
            echo "$JAVA_RUN INTVL $method $BROKERS $DEVICES $CIRCUIT $pubintvl ln2 $DELTA $seed" >> $CMD_FILE
        done
        echo "$JAVA_RUN INTVL AMQTT $BROKERS $DEVICES $CIRCUIT $pubintvl 1 $DELTA $seed" >> $CMD_FILE
    done
done

parallel --jobs 8 --progress < $CMD_FILE
rm $CMD_FILE