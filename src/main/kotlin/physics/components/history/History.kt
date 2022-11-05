package physics.components.history


class History private constructor(events: List<Event>) : Iterable<Event> {
    constructor(): this(listOf(Event.Initialization()))

    private val events = events.toMutableList()
    val size get() = eventsOfInterest().size

    private fun eventsOfInterest(): List<Event> {
        return events.filterNot { it.isEmpty() }
    }

    fun setCurrentEvent(e: Event) {
        events.add(e)
    }

    fun addPin(pin: Pin) {
        events.last().addPin(pin)
    }

    override fun iterator(): Iterator<Event> {
        return eventsOfInterest().iterator()
    }

    fun copy(): History {
        return History(events)
    }
}
