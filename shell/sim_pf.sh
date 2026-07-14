#!/bin/bash
BROKERS=100
DEVICES=1000
PUBINTVL=10
MAX_SEED=30

echo "=================================================="
echo " Lancement des simulations : Evolution du p_f"
echo "=================================================="

# --- BATCH 1 : Ligne de référence Tor et AMQTT ---
echo "-> BATCH 1: Tor et AMQTT (Circuit fixe = 3)"
rm -f cmd.tmp result.txt
for seed in $(seq 1 $MAX_SEED); do
    echo "java -jar build/libs/MQTT_Basepf1.jar CIR Tor $BROKERS $DEVICES 3 $PUBINTVL ln01 0.0001 $seed" >> cmd.tmp
    echo "java -jar build/libs/MQTT_Basepf1.jar CIR AMQTT $BROKERS $DEVICES 3 $PUBINTVL ln01 0.0001 $seed" >> cmd.tmp
done
parallel --jobs 8 --progress < cmd.tmp
mv result.txt result_base.txt

# --- BATCH 2 à 6 : Protocoles Jump (pf de 0.5 à 0.9) ---
for pf in 05 06 07 08 09; do
    echo "-> BATCH: JumpRouting & JumpMqtt avec pf=$pf"
    rm -f cmd.tmp result.txt
    for seed in $(seq 1 $MAX_SEED); do
        echo "java -jar build/libs/MQTT_Basepf${pf}.jar CIR JumpRouting $BROKERS $DEVICES 3 $PUBINTVL ln01 0.0001 $seed" >> cmd.tmp
        echo "java -jar build/libs/MQTT_Basepf${pf}.jar CIR JumpMqtt $BROKERS $DEVICES 3 $PUBINTVL ln01 0.0001 $seed" >> cmd.tmp
    done
    parallel --jobs 8 --progress < cmd.tmp
    
    # L'astuce cruciale : on renomme les tags avant de passer au pf suivant
    sed -i "s/JumpRouting/JumpRouting_pf${pf}/g" result.txt
    sed -i "s/JumpMqtt/JumpMqtt_pf${pf}/g" result.txt
    
    mv result.txt result_pf${pf}.txt
done

# --- Nettoyage et Fusion ---
rm -f cmd.tmp
echo "=================================================="
echo "Fusion de tous les résultats..."
cat result_base.txt result_pf05.txt result_pf06.txt result_pf07.txt result_pf08.txt result_pf09.txt > result_pf_total.txt
rm -f result_base.txt result_pf05.txt result_pf06.txt result_pf07.txt result_pf08.txt result_pf09.txt

echo "Terminé ! Tu peux maintenant lancer :"
echo "python3 \$HOME/bin/getavg result_pf_total.txt"
echo "=================================================="