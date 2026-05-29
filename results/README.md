# Run simulations and aggregate results

-> You need to install WSL if you are using Windows, and then install the required dependencies in the WSL environment.

- Update the package list and install the default Java Runtime Environment (JRE):
```bash
sudo apt update
sudo apt install default-jre -y
```
- Install GNU Parallel, Make, Python 3, and the necessary Python libraries (NumPy and SciPy):
```bash
sudo apt install parallel make python3
sudo apt install python3-numpy python3-scipy -y
```

- Run the simulations using GNU Parallel. The `sim_jumpmqtt.sh` script will execute the simulations in parallel, and the results will be saved in the main directory:
```bash
chmod +x ./shell/sim_jumpmqtt.sh
./shell/sim_jumpmqtt.sh
```

- After the simulations are complete, you can aggregate the results using the getavg library (obtained throught the ResearchUtil folder). This script will read the individual result files and compile them into a single aggregated results file:
```bash 
python3 $HOME/bin/getavg result_final.txt
```