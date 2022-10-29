package physics.components.history


abstract class HistoryOwner {
    private val history = History()

    fun addUpdate(trigger: String, reliesOn: List<HistoryOwner>, block: () -> Unit) {
        history.setCurrentEvent(Event.Update(trigger, relatedObjects = reliesOn))
        block()
        history.setCurrentEvent(Event.None)
    }

    fun tell(subEvent: String) {
        history.tell(subEvent)
    }

    protected abstract fun asHeader(): String

    fun toStringWithHistory(): String {
        val builder = StringBuilder()
        builder.appendLine("  < ${asHeader()} >")
        builder.appendLine()
        builder.append(history.toString())
        return builder.toString()
    }

}
