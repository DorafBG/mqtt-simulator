#!/bin/bash
# Simulations scripts for Jump Mqtt - Sakai Lab (CORRIGÉ)

JAR_PF1="build/libs/MQTT_Basepf1.jar"
JAR_PF085="build/libs/MQTT_Basepf085.jar"
JAR_PF05="build/libs/MQTT_Basepf05.jar"

BROKERS=100
DEVICES=1000
PUBINTVL=10
MAX_SEED=30
CMD_FILE="cmd_seminar.tmp"

echo "=================================================="
echo " Lancement des simulations (10 graphiques)"
echo "=================================================="

# --- EXPÉRIENCE 1 : Variation de p_f / Circuit Length ---

echo "-> BATCH 1: Tor et AMQTT (Circuits 3, 5, 7)"
rm -f $CMD_FILE result.txt
for seed in $(seq 1 $MAX_SEED); do
    for cir in 3 5 7; do
        echo "java -jar $JAR_PF1 CIR Tor $BROKERS $DEVICES $cir $PUBINTVL ln01 0.0001 $seed" >> $CMD_FILE
        echo "java -jar $JAR_PF1 CIR AMQTT $BROKERS $DEVICES $cir $PUBINTVL ln01 0.0001 $seed" >> $CMD_FILE
    done
done
parallel --jobs 8 --progress < $CMD_FILE
mv result.txt result_TorAMQTT.txt

echo "-> BATCH 2: JumpRouting & JumpMqtt avec pf=1.0"
rm -f $CMD_FILE result.txt
for seed in $(seq 1 $MAX_SEED); do
    echo "java -jar $JAR_PF1 CIR JumpRouting $BROKERS $DEVICES 3 $PUBINTVL ln01 0.0001 $seed" >> $CMD_FILE
    echo "java -jar $JAR_PF1 CIR JumpMqtt $BROKERS $DEVICES 3 $PUBINTVL ln01 0.0001 $seed" >> $CMD_FILE
done
parallel --jobs 8 --progress < $CMD_FILE
sed -i 's/JumpRouting/JumpRouting_pf1/g' result.txt
sed -i 's/JumpMqtt/JumpMqtt_pf1/g' result.txt
mv result.txt result_Jump1.txt

echo "-> BATCH 3: JumpRouting & JumpMqtt avec pf=0.85"
rm -f $CMD_FILE result.txt
for seed in $(seq 1 $MAX_SEED); do
    echo "java -jar $JAR_PF085 CIR JumpRouting $BROKERS $DEVICES 3 $PUBINTVL ln01 0.0001 $seed" >> $CMD_FILE
    echo "java -jar $JAR_PF085 CIR JumpMqtt $BROKERS $DEVICES 3 $PUBINTVL ln01 0.0001 $seed" >> $CMD_FILE
done
parallel --jobs 8 --progress < $CMD_FILE
sed -i 's/JumpRouting/JumpRouting_pf085/g' result.txt
sed -i 's/JumpMqtt/JumpMqtt_pf085/g' result.txt
mv result.txt result_Jump085.txt

echo "-> BATCH 4: JumpRouting & JumpMqtt avec pf=0.5"
rm -f $CMD_FILE result.txt
for seed in $(seq 1 $MAX_SEED); do
    echo "java -jar $JAR_PF05 CIR JumpRouting $BROKERS $DEVICES 3 $PUBINTVL ln01 0.0001 $seed" >> $CMD_FILE
    echo "java -jar $JAR_PF05 CIR JumpMqtt $BROKERS $DEVICES 3 $PUBINTVL ln01 0.0001 $seed" >> $CMD_FILE
done
parallel --jobs 8 --progress < $CMD_FILE
sed -i 's/JumpRouting/JumpRouting_pf05/g' result.txt
sed -i 's/JumpMqtt/JumpMqtt_pf05/g' result.txt
mv result.txt result_Jump05.txt

# --- EXPÉRIENCE 2 : Variation de l'intervalle (INTVL) ---
echo "-> BATCH 5: Variation Intervalle (INTVL de 10 à 60)"
rm -f $CMD_FILE result.txt
for seed in $(seq 1 $MAX_SEED); do
    for intvl in 10 20 30 40 50 60; do
        echo "java -jar $JAR_PF1 INTVL Tor $BROKERS $DEVICES 3 $intvl ln01 0.0001 $seed" >> $CMD_FILE
        echo "java -jar $JAR_PF1 INTVL AMQTT $BROKERS $DEVICES 3 $intvl ln01 0.0001 $seed" >> $CMD_FILE
        echo "java -jar $JAR_PF05 INTVL JumpRouting $BROKERS $DEVICES 3 $intvl ln01 0.0001 $seed" >> $CMD_FILE
        echo "java -jar $JAR_PF05 INTVL JumpMqtt $BROKERS $DEVICES 3 $intvl ln01 0.0001 $seed" >> $CMD_FILE
    done
done
parallel --jobs 8 --progress < $CMD_FILE
sed -i 's/JumpRouting/JumpRouting_pf05/g' result.txt
sed -i 's/JumpMqtt/JumpMqtt_pf05/g' result.txt
mv result.txt result_Intvl.txt

# --- Nettoyage et Fusion ---
rm -f $CMD_FILE
echo "=================================================="
echo "Fusion de tous les résultats dans : result_final.txt"
cat result_TorAMQTT.txt result_Jump1.txt result_Jump085.txt result_Jump05.txt result_Intvl.txt > result_final.txt
rm -f result_TorAMQTT.txt result_Jump1.txt result_Jump085.txt result_Jump05.txt result_Intvl.txt

echo "Terminé ! Tu peux maintenant lancer :"
echo "python3 \$HOME/bin/getavg result_final.txt"
echo "=================================================="