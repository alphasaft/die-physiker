package nlp


class WordUnion(
    components: List<Word>,
    override var name: String = "<unnamed>",
) : Word {

    private companion object Util {
        fun flattenComponents(components: List<Word>): List<Word> = components
            .fold(listOf()) { acc, comp -> if (comp is WordUnion) acc + comp.components else acc.plusElement(comp) }
    }

    private val components: List<Word> = flattenComponents(components)

    override fun consume(input: String): Pair<Consumed, WordInstanceList>? {
        for (word in components) {
            word.consume(input)?.also { (consumed, words) ->
                return consumed to words.map { it.copy(model = this, name = name) }
            }
        }
        return null
    }
}
