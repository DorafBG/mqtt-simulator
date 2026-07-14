
set terminal pngcairo size 800,600 enhanced font "Times New Roman,18"
# set terminal postscript eps enhanced color
unset grid
set key top left Left reverse spacing 1.2
set border 31 lw 1.5
set tics nomirror out scale 1.0

# Memes styles que sur le papier de recherche DPMQTT
set style line 1 lc rgb "black" lw 2 pt 3 ps 1.5   # AMQTT: Black Star
set style line 2 lc rgb "blue" lw 2 pt 8 ps 1.5    # Tor: Blue Open Triangle
set style line 3 lc rgb "red" lw 2 pt 6 ps 1.5     # JumpRouting: Red Open Circle
set style line 4 lc rgb "#00aa00" lw 2 pt 7 ps 1.5 # JumpMqtt: Green Filled Circle

# ================= EXPREIENCE 1 =================
set xlabel "Anonymity Level (Circuit Length / p_f)" font "Times New Roman,20" offset 0,-0.5
set xtics ("cir=3/pf=0.5" 3, "cir=5/pf=0.85" 5, "cir=7/pf=1.0" 7)

set ylabel "Delivery Rate" font "Times New Roman,20" offset -1,0
set output "exp1_delivery.png"
plot "exp1_delivery.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (baseline)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (proposed)" with linespoints ls 4

set ylabel "End-to-End Delay (ms)" font "Times New Roman,20" offset -1,0
set output "exp1_delay.png"
plot "exp1_delay.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (baseline)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (proposed)" with linespoints ls 4

set ylabel "Total Number of Hops" font "Times New Roman,20" offset -1,0
set output "exp1_hops.png"
plot "exp1_hops.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (baseline)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (proposed)" with linespoints ls 4

set ylabel "Amount of Traffic (Bytes)" font "Times New Roman,20" offset -1,0
set output "exp1_traffic.png"
plot "exp1_traffic.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (baseline)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (proposed)" with linespoints ls 4

set ylabel "Crypto Ops per Broker" font "Times New Roman,20" offset -1,0
set output "exp1_crypto.png"
plot "exp1_crypto.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (baseline)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (proposed)" with linespoints ls 4

# ================= EXPERIENCE 2 =================
set xlabel "Message Publication Interval [sec.]" font "Times New Roman,20" offset 0,-0.5
set xtics auto

set ylabel "Delivery Rate" font "Times New Roman,20" offset -1,0
set output "exp2_delivery.png"
plot "exp2_delivery.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (p_f=0.5)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (p_f=0.5)" with linespoints ls 4

set ylabel "End-to-End Delay (ms)" font "Times New Roman,20" offset -1,0
set output "exp2_delay.png"
plot "exp2_delay.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (p_f=0.5)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (p_f=0.5)" with linespoints ls 4

set ylabel "Total Number of Hops" font "Times New Roman,20" offset -1,0
set output "exp2_hops.png"
plot "exp2_hops.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (p_f=0.5)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (p_f=0.5)" with linespoints ls 4

set ylabel "Amount of Traffic (Bytes)" font "Times New Roman,20" offset -1,0
set output "exp2_traffic.png"
plot "exp2_traffic.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (p_f=0.5)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (p_f=0.5)" with linespoints ls 4

set ylabel "Crypto Ops per Broker" font "Times New Roman,20" offset -1,0
set output "exp2_crypto.png"
plot "exp2_crypto.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (p_f=0.5)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (p_f=0.5)" with linespoints ls 4
