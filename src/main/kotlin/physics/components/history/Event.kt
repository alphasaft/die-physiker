package physics.components.history


sealed class Event(val relatedHistories: List<(HistoryFormatter) -> String>) : Iterable<Pin> {
    private val pins = mutableListOf<Pin>()

    fun addPin(pin: Pin) {
        pins.add(pin)
    }

    override fun iterator(): Iterator<Pin> {
        return pins.iterator()
    }

    open fun isEmpty(): Boolean = pins.isEmpty()

    object None : Event(emptyList()) { override fun isEmpty(): Boolean = true }
    class Initialization : Event(emptyList())
    class Update(val trigger: String, relatedObjects: List<HistoryOwner>) : Event(relatedObjects.map { it.snapshot() })
}
