#!/bin/bash
# Simulations scripts for Jump Mqtt - Sakai Lab

# Chemins vers tes 3 fichiers JAR
JAR_PF1="build/libs/MQTT_Basepf1.jar"
JAR_PF085="build/libs/MQTT_Basepf085.jar"
JAR_PF05="build/libs/MQTT_Basepf05.jar"

# Paramètres
BROKERS=100
DEVICES=1000
CIRCUIT=3
PUBINTVL=10
MAX_SEED=100
CMD_FILE="cmd_seminar.tmp"

echo "=================================================="
echo " Lancement des simulations (500 runs au total)"
echo "=================================================="

# --- BATCH 1: PlainMQTT et Tor ---
echo "-> BATCH 1: PlainMQTT et Tor (100 runs chacun)"
rm -f $CMD_FILE result.txt
for seed in $(seq 1 $MAX_SEED); do
    echo "java -jar $JAR_PF1 CIR PlainMQTT $BROKERS $DEVICES $CIRCUIT $PUBINTVL ln01 0.0001 $seed" >> $CMD_FILE
    echo "java -jar $JAR_PF1 CIR Tor $BROKERS $DEVICES $CIRCUIT $PUBINTVL ln01 0.0001 $seed" >> $CMD_FILE
done
parallel --jobs 8 --progress < $CMD_FILE
mv result.txt result_PlainTor.txt

# --- BATCH 2: JumpMqtt pf=1.0 ---
echo "-> BATCH 2: JumpMqtt avec pf=1.0"
rm -f $CMD_FILE result.txt
for seed in $(seq 1 $MAX_SEED); do
    echo "java -jar $JAR_PF1 CIR JumpMqtt $BROKERS $DEVICES $CIRCUIT $PUBINTVL ln01 0.0001 $seed" >> $CMD_FILE
done
parallel --jobs 8 --progress < $CMD_FILE
# On renomme le tag dans le résultat pour l'outil getavg du prof
sed -i 's/JumpMqtt/JumpMqtt_pf1/g' result.txt
mv result.txt result_Jump1.txt

# --- BATCH 3: JumpMqtt pf=0.85 ---
echo "-> BATCH 3: JumpMqtt avec pf=0.85"
rm -f $CMD_FILE result.txt
for seed in $(seq 1 $MAX_SEED); do
    echo "java -jar $JAR_PF085 CIR JumpMqtt $BROKERS $DEVICES $CIRCUIT $PUBINTVL ln01 0.0001 $seed" >> $CMD_FILE
done
parallel --jobs 8 --progress < $CMD_FILE
# On renomme le tag
sed -i 's/JumpMqtt/JumpMqtt_pf085/g' result.txt
mv result.txt result_Jump085.txt

# --- BATCH 4: JumpMqtt pf=0.5 ---
echo "-> BATCH 4: JumpMqtt avec pf=0.5"
rm -f $CMD_FILE result.txt
for seed in $(seq 1 $MAX_SEED); do
    echo "java -jar $JAR_PF05 CIR JumpMqtt $BROKERS $DEVICES $CIRCUIT $PUBINTVL ln01 0.0001 $seed" >> $CMD_FILE
done
parallel --jobs 8 --progress < $CMD_FILE
# On renomme le tag
sed -i 's/JumpMqtt/JumpMqtt_pf05/g' result.txt
mv result.txt result_Jump05.txt

# --- Nettoyage et Fusion ---
rm -f $CMD_FILE
echo "=================================================="
echo "Fusion de tous les résultats dans : result_final.txt"
cat result_PlainTor.txt result_Jump1.txt result_Jump085.txt result_Jump05.txt > result_final.txt
rm -f result_PlainTor.txt result_Jump1.txt result_Jump085.txt result_Jump05.txt

echo "Terminé ! Tu peux maintenant calculer les moyennes avec l'outil du prof :"
echo "python3 \$HOME/bin/getavg result_final.txt"
echo "=================================================="