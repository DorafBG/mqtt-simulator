# 🚀 QUICK START - Lancer la simu en 2 minutes

## Étape 1 : Compiler
```powershell
cd "C:\Users\quent\OneDrive - Université Paris-Saclay\Bureau\DPMQTT_Sim\DPMQTT_Sim"
.\gradlew.bat build
```

**Sortie attendue** : ✅ BUILD SUCCESSFUL

---

## Étape 2 : Première simulation (DP-MQTT)

```powershell
java -jar build/libs/MQTT_Base.jar DELTA DPMQTT 100 1000 3 60 ln2 0.001 1
```

**Paramètres** :
- `DELTA` = Scenario d'étude du delta
- `DPMQTT` = Protocole à tester
- `100` = 100 courtiers
- `1000` = 1000 appareils IoT
- `3` = Longueur du circuit (sauts)
- `60` = Intervalle de publication (secondes)
- `ln2` = Epsilon (budget de privacité)
- `0.001` = Delta (proba dépassement)
- `1` = Graine aléatoire (reproductibilité)

**Durée** : ~5-30 secondes selon votre machine

**Résultat** : ✅ Une ligne ajoutée à `result.txt`

---

## Étape 3 : Consulter les résultats

```powershell
Get-Content result.txt -Tail 1
```

**Sortie** : 
```
DELTA_DPMQTT_100_1000_ln02_00001_3_00060 1 1000 987 98.7 45.3 5432 234567 98 4567 5530 238134 4521 3456 23.4 12.1
```

**Interprétation** :
- 1000 messages initiés
- 987 livrés = 98.7% taux de livraison ✅
- Délai moyen = 45.3 ms
- 4521 chiffrements
- 3456 déchiffrements

---

## 🎯 Tester les 6 protocoles

Copier-coller ce script PowerShell :

```powershell
$protocols = @("PlainMQTT", "AMQTT", "Tor", "DPMQTT", "OptDPMQTT", "Vuvuzela")

foreach ($prot in $protocols) {
    Write-Host "Testing $prot..." -ForegroundColor Green
    java -jar build/libs/MQTT_Base.jar CIR $prot 100 1000 3 60 ln2 0.001 1
}

Write-Host "Results in result.txt :" -ForegroundColor Green
Get-Content result.txt | Tail -6
```

**Sortie** : 6 lignes avec résultats pour chaque protocole

---

## 📊 Analyser avec Excel

1. Ouvrir `result.txt` avec Excel
2. Délimiteur = Espace
3. Colonnes :
   - A: Scenario + protocole + params
   - B-E: Seed + Métriques livraison
   - F-M: Métriques trafic
   - N-O: Métriques crypto

---

## 🔄 Étudier l'impact du paramètre epsilon (confidentialité)

```powershell
$epsilons = @("ln2", "ln3", "ln5", "ln10")

foreach ($eps in $epsilons) {
    Write-Host "Testing epsilon=$eps..." -ForegroundColor Cyan
    java -jar build/libs/MQTT_Base.jar EPS DPMQTT 100 1000 3 60 $eps 0.001 1
}

# Plus epsilon augmente → moins de bruit → délai diminue
```

---

## 📈 Reproduire un résultat exactement

Utilisez la **même graine** (seed) :

```powershell
# Première exécution
java -jar build/libs/MQTT_Base.jar DELTA DPMQTT 100 1000 3 60 ln2 0.001 5

# Deuxième exécution - résultat identique
java -jar build/libs/MQTT_Base.jar DELTA DPMQTT 100 1000 3 60 ln2 0.001 5
```

---

## 🧪 Comparaison rapide

```powershell
# PlainMQTT vs DP-MQTT
Write-Host "PlainMQTT (baseline):" -ForegroundColor Yellow
java -jar build/libs/MQTT_Base.jar CIR PlainMQTT 50 500 3 60 ln2 0.001 1

Write-Host "`nDP-MQTT (avec anonymité + bruit):" -ForegroundColor Yellow
java -jar build/libs/MQTT_Base.jar CIR DPMQTT 50 500 3 60 ln2 0.001 1

# DP-MQTT aura plus de trafic mais meilleures garanties de privacy
```

---

## 📍 Fichier généré

```
result.txt
├─ PlainMQTT_CIR_50_500_ln02_00001_3_00060 1 500 495 99.0 5.2 1234 56789 0 0 1234 56789 456 234 1.2 0.8
├─ DPMQTT_CIR_50_500_ln02_00001_3_00060 1 500 489 97.8 23.5 1434 67234 56 3421 1490 70655 823 567 4.5 2.3
└─ ... (3 autres protocoles)
```

---

## 🔐 Comprendre les résultats

| Protocole | Délai | Trafic | Sécurité |
|-----------|-------|--------|----------|
| **PlainMQTT** | ⚡ Bas | 📉 Bas | ❌ Aucune |
| **AMQTT** | 🔴 Haut | 📈 Moyen | ✅ Anonyme |
| **DP-MQTT** | 🔴 Très haut | 📈📈 Élevé | ✅✅ Anonyme + Privé |

---

## ⚙️ Si ça ne marche pas

### Erreur : JAR non trouvé
```powershell
# Vérifier que la compilation a réussi
.\gradlew.bat build

# Vérifier que le JAR existe
Test-Path build/libs/MQTT_Base.jar
```

### Erreur : "Invalid number of args"
```
Vous avez oublié un argument (besoin de 9)
java -jar build/libs/MQTT_Base.jar DELTA DPMQTT 100 1000 3 60 ln2 0.001 1
                                       ^      ^    ^   ^    ^ ^ ^  ^    ^
                                       1      2    3   4    5 6 7  8    9
```

### Erreur : Seed invalide
```
Seed doit être entier > 0
java -jar build/libs/MQTT_Base.jar DELTA DPMQTT 100 1000 3 60 ln2 0.001 abc
                                                                           ^^^
                                                                  Doit être 1-999
```

---

## 📌 Résumé

✅ **Compiler** : `.\gradlew.bat build`
✅ **Tester** : `java -jar build/libs/MQTT_Base.jar DELTA DPMQTT 100 1000 3 60 ln2 0.001 1`
✅ **Résultats** : `Get-Content result.txt -Tail 1`
✅ **Comparer** : Lancer avec différents protocoles
✅ **Analyser** : Excel ou Python sur result.txt

---

## 🎓 Prochaines lectures

1. 📖 **EXPLICATIONS_PROJET_DPMQTT.md** - Vue d'ensemble complète
2. 🚀 **GUIDE_LANCER_SIMULATION.md** - Tous les paramètres
3. 🔄 **FONCTIONNEMENT_SIMULATION.md** - Comment ça marche
4. ⚡ **RESUME_5MIN.md** - Condensé rapide

