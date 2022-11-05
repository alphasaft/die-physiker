package physics.components.history

object StandardHistoryFormatter : AbstractHistoryFormatter() {
    override fun nThEventHeader(n: Int): String = "$n -----\n"
    override fun formatInitializationEvent(event: Event.Initialization): String? = null
    override fun formatPinDefault(pin: Pin): String = pin.content
    override fun formatUpdateEvent(event: Event.Update): String = "[ ${event.trigger} ]"
}
