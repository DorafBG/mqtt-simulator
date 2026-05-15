package jp.ac.tmu.sakailab.mqtt

import java.util.PriorityQueue

/**
 * The discrete event scheduler that manages the simulation clock and event queue.
 * This class is a singleton implemented as a Kotlin [object].
 * @author Kazuya Sakai, Ph.D.
 */
open class Scheduler {
    companion object {
        /**
         * The current time in the simulation (unit depends on implementation, e.g., msec).
         */
        var time: Long = 0
            internal set // Public read, internal write

        /**
         * A priority queue of scheduled events, sorted by their execution time.
         */
        private val eventQueue = PriorityQueue<Event>()


        /**
         * Adds a single event to the scheduler.
         * @param e The event to be scheduled.
         */
        fun addEvent(e: Event) {
            eventQueue.add(e)
        }

        /**
         * Adds a list of events to the scheduler.
         * @param eventsList The list of events to be scheduled.
         */
        fun addEvents(eventsList: List<Event>) {
            eventQueue.addAll(eventsList)
        }

        /**
         * Retrieves and removes the next event from the queue.
         * @return The next event, or null if the queue is empty.
         */
        private fun nextEvent(): Event? {
            return eventQueue.poll()
        }

        /**
         * Cancels a specific event by object reference.
         * @param e The event to be removed.
         */
        fun cancelEvent(e: Event) {
            eventQueue.remove(e)
        }

        /**
         * Cancels an event by its unique ID.
         * @param eventId The unique ID of the event to be removed.
         * @return True if the event was found and removed, false otherwise.
         */
        fun cancelEvent(eventId: Int): Boolean {
            return eventQueue.removeIf { it.id == eventId }
        }

        /**
         * Clears all scheduled events from the queue.
         */
        fun cancelAllEvents() {
            eventQueue.clear()
        }

    }

    /**
     * Starts the simulation and executes all events until the queue is empty.
     */
    fun run() {
        while (eventQueue.isNotEmpty()) {
            val e = nextEvent() ?: break
            time = e.time
            e.callback(e.params)
        }
    }

    /**
     * Progresses the simulation by a [unitTime].
     * Executes all events scheduled within the time window.
     * @param unitTime The duration of the step.
     * @return True if there are more events in the queue, false otherwise.
     */
    fun step(unitTime: Long): Boolean {
        if (eventQueue.isEmpty()) return false

        val tempTime = time
        while (eventQueue.isNotEmpty()) {
            val e = eventQueue.peek()
            if (tempTime + unitTime < e.time) break

            eventQueue.poll()
            time = e.time
            e.callback(e.params)
        }
        time = tempTime + unitTime
        return true
    }
}