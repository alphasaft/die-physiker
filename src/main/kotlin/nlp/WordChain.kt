package nlp

import dto.TokenList
import dto.WordInstance
import dto.WordInstanceList
import dto.values
import println

class WordChain(
    components: List<Word>,
    override var name: String = "<unnamed>",
    private var concatenate: WordChain.(WordInstanceList) -> WordInstanceList = Util::defaultConcatenateToString
) : Word {

    companion object Util {
        private fun WordChain.concatenateImpl(words: WordInstanceList, valueTransformer: (List<Any>) -> Any): WordInstanceList {
            if (words.isEmpty()) return words
            return listOf(WordInstance(
                model = words.first().model,
                name =  this.name,
                value = valueTransformer(words.values),
                start = words.minOf { it.start }
            ))
        }

        fun defaultConcatenate(receiver: WordChain, words: WordInstanceList) = receiver.concatenateImpl(words) { it }
        fun defaultConcatenateToString(receiver: WordChain, words: WordInstanceList) = receiver.concatenateImpl(words) { it.joinToString(" ") }

        fun flattenComponents(components: List<Word>): List<Word> {
            return components.fold(listOf()) { acc, comp ->
                if (comp is WordChain) acc + comp.components else acc.plusElement(comp)
            }
        }
    }

    private val components: List<Word> = flattenComponents(components)

    override fun consume(tokens: TokenList): Pair<Consumed, WordInstanceList>? {
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