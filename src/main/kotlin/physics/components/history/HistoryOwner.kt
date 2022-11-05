package physics.components.history


abstract class HistoryOwner {
    private val history = History()

    abstract fun representForHistory(): String

    fun addUpdate(trigger: String, reliesOn: List<HistoryOwner>, block: () -> Unit) {
        history.setCurrentEvent(Event.Update(trigger, relatedObjects = reliesOn))
        block()
        history.setCurrentEvent(Event.None)
    }

    fun addPin(content: String, metadata: String? = null) {
        history.addPin(Pin(content, metadata))
    }

    fun formatHistory(formatter: HistoryFormatter = StandardHistoryFormatter): String {
        return "< ${representForHistory()} > \n\n${formatter.format(history)}"
    }

    fun snapshot(): (HistoryFormatter) -> String {
        val representation = representForHistory()
        val historyCopy = history.copy()
        return { f -> "< $representation > \n\n ${f.format(historyCopy)}" }
    }
}
