package nlp

internal object UnknownWord : Word {
    override val name = "<unknown>"
    override fun consume(input: String): Pair<Consumed, WordInstanceList> {
        return if (input.first().isLetter()) {
            input.takeWhile { it.isLetter() }.let { it.length to listOf(WordInstance(this, name, it)) }
        } else {
            1 to listOf(WordInstance(this, name, input.first()))
        }
    }
}
