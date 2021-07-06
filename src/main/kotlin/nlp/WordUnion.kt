package nlp

import dto.Token
import dto.TokenList
import dto.WordInstance
import dto.WordInstanceList
import println

class WordUnion(
    components: List<Word>,
    override var name: String = "<unnamed>",
) : Word {

    private companion object Util {
        fun flattenComponents(components: List<Word>): List<Word> = components
            .fold(listOf()) { acc, comp -> if (comp is WordUnion) acc + comp.components else acc.plusElement(comp) }
    }

    private val components: List<Word> = flattenComponents(components)

    override fun consume(tokens: TokenList): Pair<Consumed, WordInstanceList>? {
        for (word in components) word.consume(tokens)?.also { (consumed, words) ->
            return consumed to words.map { it.copy(model = this, name = name) }
        }
        return null
    }
}
