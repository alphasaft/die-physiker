package physics.components.history

class History {
    private val events = mutableListOf<Event>(Event.Initialization())

    fun setCurrentEvent(e: Event) {
        events.add(e)
    }

    fun tell(subEvent: String) {
        events.last().tell(subEvent)
    }

    override fun toString(): String {
        val builder = StringBuilder()
        var n = 1
        for (event in events.filterNot(Event::isEmpty)) {
            builder.append("$n ----\n\n")
            builder.append(event.summary())
            builder.append("\n\n")
            n++
        }
        return builder.toString()
    }
}
