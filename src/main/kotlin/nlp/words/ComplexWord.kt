package nlp.words

import nlp.Consumed
import nlp.WordList
import tokenizing.TokenList
import util.DslInitializer
import util.MayBeInitializedByDsl

class ComplexWord(
    components: List<Word>,
    @MayBeInitializedByDsl override var name: String = "<not named>",
    @MayBeInitializedByDsl private var concatenate: ComplexWord.(WordList) -> WordList = { listOf(it.reduce { acc, word -> WordInstance(acc.word, name, acc.value.toString() to word.value.toString(), acc.start) }) }
) : Word {

    @DslInitializer("name")
    infix fun named(name: String) = apply { this.name = name }

    @DslInitializer("concatenate")
    infix fun andConcatenateWith(concatenate: ComplexWord.(WordList) -> WordList) = apply { this.concatenate = concatenate }

    private val components: List<Word> = components
        .fold(listOf()) { acc, elem -> if (elem is ComplexWord) acc + elem.components else acc.plusElement(elem) }

    override fun consume(tokens: TokenList): Pair<Consumed, WordList>? {
        var remainingTokens = tokens
        val result = mutableListOf<WordInstance>()
        var totalConsumption: Consumed = 0

        for (component in components) {
            val (consumed, words) = component.consume(remainingTokens) ?: return null
            result.addAll(words)
            totalConsumption += consumed
            remainingTokens = remainingTokens.subList(consumed, remainingTokens.size)
        }

        return totalConsumption to concatenate(result)
    }
}