package jp.ac.tmu.sakailab.mqtt.routing

import jp.ac.tmu.sakailab.mqtt.Broker
import jp.ac.tmu.sakailab.mqtt.Device
import jp.ac.tmu.sakailab.mqtt.Config

/**
 * This is a set of functions for public bulletin board that serves as an oracle.
 *
 * @author Kazuya Sakai, Ph.D.
 */
object PublicBulletinBoard {

    /**
     * The map between a topic ID and a set of subscriber's IDs.
     * <topicId, the destination device ID set>
     */
    val destDeviceIdTable: MutableMap<Int, Set<Int>> = sortedMapOf()

    /**
     * The map between a topic ID and a set of destination broker IDs associated with at least
     * destination device.
     * <topicID, the destination broker ID set associated with at least one destination device>
     */
    val destBrokerIdTable: MutableMap<Int, Set<Int>> = sortedMapOf()

    /**
     * This function initializes the destination device ID table for each topic ID.
     * @param numTopics The number of topic IDs in the system.
     */
    fun initDestDeviceIdsTable(numTopics: Int) {
        repeat(numTopics) { i ->
            destDeviceIdTable[i] = getDestDeviceIds(i)
        }
    }

    /**
     * This function initializes the destination broker ID table for each topic ID.
     * @param numTopics The number of topic IDs in the system.
     */
    fun initDestBrokerIdTable(numTopics: Int) {
        repeat(numTopics) { i ->
            destBrokerIdTable[i] = getDestBrokerIds(i)
        }
    }

    /**
     * This function returns the device IDs who are the subscribers of a given topic ID.
     * @param topicId A topic ID.
     * @return Returns a set of device IDs Set<Int>
     */
    fun getDestDeviceIds(topicId: Int): Set<Int> {
        val deviceList = Config.deviceList ?: return emptySet()

        return deviceList
            .filter { it.subscTopics.contains(topicId) }
            .map { it.id }
            .toSortedSet()
    }

    /**
     * This function returns the broker ID for a given device ID.
     * @param deviceId A device ID
     * @return Returns the associated broker ID of a given device.
     */
    fun getDestBrokerId(deviceId: Int): Int {
        val deviceList = Config.deviceList ?: return -1
        val node = deviceList.find { it.id == deviceId }

        return (node)?.associatedBrokerId ?: -1
    }

    /**
     * This function returns a set of broker IDs at least on of which associated device
     * subscribes a given topic.
     * @param topicId A topic ID.
     * @return Returns A set of broker IDs, Set<Int>.
     */
    fun getDestBrokerIds(topicId: Int): Set<Int> {
        val deviceList = Config.deviceList ?: return emptySet()

        return deviceList
            .filterIsInstance<Device>() // filter as a Device interface
            .filter { it.subscTopics.contains(topicId) } // check with topic IDs
            .map { it.associatedBrokerId } // get broker IDs in a type-safe fashion.
            .toSortedSet()
    }

    /**
     * This function computes the sensitivity of a given broker.
     * @param broker A broker instance.
     * @return Returns the sensitivity of a given broker, i.e., the maximum number of destination devices.
     */
    fun getSensitivity(broker: Broker): Int {
        var maxNumDest = 0

        broker.associatedDevices.values.forEach { device ->
            device.subscTopics.forEach { topicId ->
                // handling when destBrokerIdTable[topicId] is null
                val tmp = destBrokerIdTable[topicId]?.size ?: 0
                if (tmp > maxNumDest) {
                    maxNumDest = tmp
                }
            }
        }

        return maxNumDest
    }
}