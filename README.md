# Anonymous MQTT Project

## About
* Directed by Kazuya Sakai, Ph.D.
* Affiliation: Tokyo Metropolitan University
* This repository includes a suit of anonymous MQTT protocols implemented by Kotlin.
* Our simulations are event-driven and focus on application-layer routing.

## Disclaimer
* Delay: The end-to-end delay highly depends on the physical layer assumptions, the cryptographic processing delay assumptions, computing resources, e.g., the number of CPU cores. Therefore, the results obtaiend by this simulations could be different from published papers. Howerever, we are sure that you will be ablt to obtain mostly the same resutls regarding the number of transmitted messages, amonts of traffic, and the number of encryption/decryption operations, which are protocol-dependent metrics.
* Delivery rates: When the message queue size (in the application layer) is not sufficiently large, it may drop the packets. Since the queue state is also affected by physical layer assumptions and buffer size. Therefore, you may obtain different resutls from the ones presented in our papers, when 100% delivery is not guranteed.
* jp.ac.tmu.sakailab.mqtt.priv.ExpMechanism has not been completed. (We have not used this module in our papers yet.)

### Plain MQTT
* Plain MQTT without anonymity
* Implementation: jp.ac.tmu.sakailab.mqtt.routing.plainmqt

## Anonymous MQTT Protocols
* Anonymous MQTT protocol family ensures anonymity against various adversary models.

### A-MQTT (Anonymous MQTT)
* Anonymous MQTT (A-MQTT) ensures source/destination/path anonymity and untraceable rates.
* Reference: Y. Fukushima, H. Tsunamoto, K. Sakai, M.-T. Sun, and W.-S. Ku, ''An Analysis of Anonymous MQTT for Publish-Subscribe-Based IoT Networks,'' IEEE Trans. Netw. Sci. Eng, vol. 12, no. 4, pp. 3206-3220, 2025.
* Implementation: jp.ac.tmu.sakailab.mqtt.routing

### Tor (Tailored Tor)
* Tor protocol tailored to the MQTT context.
* Reference: J. Hiller, J. Pennekamp, M. Dahlmanns, M. Henze, A. Panchenko, and
  K. Wehrle, ''Tailoring Onion Routing to the Internet of Things: Security
  and Privacy in Untrusted Environments,'' in ICNP, 2019, pp. 1–12.
Implementation: jp.ac.tmu.sakailab.mqtt.routing.tor

## Differentially Private MQTT Protocols
* Differentially private MQTT protocol family ensures both anonymity and differentially private against various adversary models.

### DP-MQTT (Differentially-private MQTT)
* DP-MQTT adds noise messages, which is implemented on top of A-MQTT.
* Reference:
* Implementation: jp.ac.tmu.sakailab.mqtt.routing.dpmqtt

### Private Messaging-based Protocol
* Vuvuzela is tailored to the MQTT context
* Reference: J. V. D. Hooff, D. Lazar, M. Zaharia, N. Zeldovich, "Vuvuzela: Scalable private messaging resistant to traffic analysis", In SOSP 2015.
* Implementation: jp.ac.tmu.sakailab.mqtt.routing.privmsg