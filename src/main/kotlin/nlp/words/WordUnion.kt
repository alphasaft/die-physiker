package nlp.words

import nlp.Consumed
import nlp.WordList
import tokenizing.TokenList
import util.DslInitializer
import util.MayBeInitializedByDsl
import util.MayBeMutatedByDsl

class WordUnion(
    components: List<Word>,
    @MayBeInitializedByDsl override var name: String = "<not named>",
) : Word {
    @DslInitializer("name")
    infix fun named(name: String) = apply { this.name = name }

    private val components: List<Word> = components
        .fold(listOf()) { acc, elem -> if (elem is WordUnion) acc + elem.components else acc.plusElement(elem) }

    override fun consume(tokens: TokenList): Pair<Consumed, WordList>? {
        for (word in components) {
            word.consume(tokens)?.also { return it }
        }
        return null
    }


}