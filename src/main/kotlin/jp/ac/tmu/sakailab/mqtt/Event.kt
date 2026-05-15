package jp.ac.tmu.sakailab.mqtt

/**
 * Represents a simulation event in the discrete event simulator.
 * This class replaces the reflection-based approach with type-safe lambdas.
 * * @property time The scheduled time for the event.
 * @property callback The functional block to be executed.
 * @property params The arguments for the callback function.
 * @author Kazuya Sakai, Ph.D.
 */
class Event(
    var time: Long,
    val callback: (Array<Any?>) -> Unit,
    val params: Array<Any?>
) : Comparable<Event> {

    companion object {
        private var eventIdCounter = 0
    }

    /** Automatically generated event ID. */
    val id: Int = eventIdCounter++

    /** Events are ordered in the increasing order of their timestamp. */
    override fun compareTo(other: Event): Int {
        return when {
            this.time != other.time -> this.time.compareTo(other.time)
            else -> this.id.compareTo(other.id)
        }
    }

    override fun toString(): String {
        return "$time [Event ID: $id]"
    }
}