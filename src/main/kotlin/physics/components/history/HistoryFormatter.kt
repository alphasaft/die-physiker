package physics.components.history

fun interface HistoryFormatter {
    fun format(history: History): String
}
