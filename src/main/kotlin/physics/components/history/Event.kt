package physics.components.history

sealed class Event {

    abstract fun tell(message: String)
    abstract fun summary(): String
    abstract fun isEmpty(): Boolean

    object None : Event() {
        override fun tell(message: String) {}
        override fun summary(): String = "<?>"
        override fun isEmpty(): Boolean = true
    }

    class Initialization : Event() {
        private val actions = mutableListOf<String>()

        override fun tell(message: String) {
            actions.add(message)
        }

        override fun summary(): String {
            return actions.joinToString("\n")
        }

        override fun isEmpty(): Boolean = actions.isEmpty()
    }

    class Update(private val trigger: String, private val relatedObjects: List<HistoryOwner>) : Event() {
        private val subEvents: MutableList<String> = mutableListOf()

        override fun tell(message: String) {
            subEvents.add(message)
        }

        override fun summary(): String {
            val builder = StringBuilder()
            val formattedRelatedHistories = relatedObjects.joinToString("\n\n") { "|" + it.toStringWithHistory().replace("\n", "\n| ") }
            val formattedSubEvents = subEvents.joinToString("\n")

            builder.append("$trigger\n\n")
            builder.append(formattedRelatedHistories)
            builder.append("\n\n->\n\n")
            builder.append(formattedSubEvents)

            return builder.toString()
        }

        override fun isEmpty(): Boolean = subEvents.isEmpty()
    }
}