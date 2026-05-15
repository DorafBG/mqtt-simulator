package jp.ac.tmu.sakailab.mqtt


/**
 * Interface representing a Broker in the MQTT simulation.
 * Responsible for managing associated devices and connected brokers.
 *
 * @author Kazuya Sakai, Ph.D.
 */
interface Broker {
    /** The unique ID of this broker. */
    val id: Int

    /** Associated IoT devices managed by this broker. */
    val associatedDevices: Map<Int, Device>

    /** IDs of the associated IoT devices. */
    val associatedDeviceIds: Set<Int>

    /** Neighboring brokers connected in the network. */
    val connectedBrokers: Map<Int, Broker>

    /** Associates a new device with this broker. */
    fun setAssociatedDevice(device: Device)

    /** Establishes a connection with another broker. */
    fun setConnectedBrokers(broker: Broker)
}