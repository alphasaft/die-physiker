package physics.components.history


// Assuming after every user-defined formatting, a linebreak is added.
abstract class AbstractHistoryFormatter : HistoryFormatter {
    open val nestedHistoriesPrefix: String = "| "
    open val separationBetweenNestedHistoriesAndPins: String = "->\n"

    open val pinsFormatMethodsByMetadata: Map<String, (String) -> String> = emptyMap()
    abstract fun formatPinDefault(pin: Pin): String

    abstract fun nThEventHeader(n: Int): String
    abstract fun formatInitializationEvent(event: Event.Initialization): String?
    abstract fun formatUpdateEvent(event: Event.Update): String?

    private fun formatPin(pin: Pin): String {
        val metadata = pin.metadata
        if (metadata == null || metadata !in pinsFormatMethodsByMetadata) return formatPinDefault(pin)
        return pinsFormatMethodsByMetadata.getValue(metadata)(pin.content)
    }

    private fun formatEvent(event: Event): String {
        val builder = StringBuilder()

        (when (event) {
            is Event.Initialization -> formatInitializationEvent(event)
            is Event.Update -> formatUpdateEvent(event)
            else -> throw NoWhenBranchMatchedException()
        })?.let { builder.appendLine(it).appendLine() }

        if (event.relatedHistories.isNotEmpty()) {
            val relatedObjectsHistoriesBuilder = StringBuilder()
            for (historySnapshot in event.relatedHistories) {
                val formattedHistory = nestedHistoriesPrefix + historySnapshot(this).replace("\n", "\n$nestedHistoriesPrefix")
                relatedObjectsHistoriesBuilder.appendLine(formattedHistory.dropLast(2*nestedHistoriesPrefix.length + 1))
            }
            builder.append(relatedObjectsHistoriesBuilder)
            builder.appendLine(separationBetweenNestedHistoriesAndPins)
        }

        val pinsBuilder = StringBuilder()
        for (pin in event) {
            pinsBuilder.appendLine(formatPin(pin))
        }
        builder.append(pinsBuilder)
        return builder.toString()
    }

    override fun format(history: History): String {
        val builder = StringBuilder()

        for ((i, event) in history.withIndex()) {
            if (history.size != 1) builder.appendLine(nThEventHeader(i+1))
            builder.appendLine(formatEvent(event))
        }

        return builder.toString()
    }
}