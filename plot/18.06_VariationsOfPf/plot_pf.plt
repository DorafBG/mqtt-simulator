
set terminal pngcairo size 800,600 enhanced font "Times New Roman,18"
unset grid
set key top left Left reverse spacing 1.2
set border 31 lw 1.5
set tics nomirror out scale 1.0

set style line 1 lc rgb "black" lw 2 pt 3 ps 1.5   
set style line 2 lc rgb "blue" lw 2 pt 8 ps 1.5    
set style line 3 lc rgb "red" lw 2 pt 6 ps 1.5     
set style line 4 lc rgb "#00aa00" lw 2 pt 7 ps 1.5 

set xlabel "Forwarding Probability (p_f)" font "Times New Roman,20" offset 0,-0.5
set xtics (0.5, 0.6, 0.7, 0.8, 0.9)
set xrange [0.45:0.95]

set ylabel "Delivery Rate" font "Times New Roman,20" offset -1,0
set output "pf_exp_delivery.png"
plot "pf_exp_delivery.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (baseline)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (proposed)" with linespoints ls 4

set ylabel "End-to-End Delay (ms)" font "Times New Roman,20" offset -1,0
set output "pf_exp_delay.png"
plot "pf_exp_delay.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (baseline)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (proposed)" with linespoints ls 4

set ylabel "Total Number of Hops" font "Times New Roman,20" offset -1,0
set output "pf_exp_hops.png"
plot "pf_exp_hops.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (baseline)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (proposed)" with linespoints ls 4

set ylabel "Amount of Traffic (Bytes)" font "Times New Roman,20" offset -1,0
set output "pf_exp_traffic.png"
plot "pf_exp_traffic.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (baseline)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (proposed)" with linespoints ls 4

set ylabel "Crypto Ops per Broker" font "Times New Roman,20" offset -1,0
set output "pf_exp_crypto.png"
plot "pf_exp_crypto.dat" using 1:2 title "AMQTT" with linespoints ls 1, \
     "" using 1:3 title "Tor" with linespoints ls 2, \
     "" using 1:4 title "JumpRouting (baseline)" with linespoints ls 3, \
     "" using 1:5 title "JumpMqtt (proposed)" with linespoints ls 4
