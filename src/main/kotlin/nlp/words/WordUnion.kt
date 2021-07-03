package nlp.words

import dto.TokenList
import dto.WordInstanceList
import nlp.Consumed

class WordUnion(
    components: List<Word>,
    override var name: String,
) : Word {

    private companion object Util {
        fun flattenComponents(components: List<Word>): List<Word> = components
            .fold(listOf()) { acc, comp -> if (comp is WordUnion) acc + comp.components else acc.plusElement(comp) }
    }

    private val components: List<Word> = flattenComponents(components)

    override fun consume(tokens: TokenList): Pair<Consumed, WordInstanceList>? {
        for (word in components) word.consume(tokens)?.also { return it }
        return null
    }
}
