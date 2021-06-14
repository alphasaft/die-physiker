package nlp

import nlp.words.UnknownWord
import nlp.words.Word
import nlp.words.WordInstance
import tokenizing.TokenList
import util.DslFunction
import util.DslScopeFunction


class Corpus(vararg words: Word) {
    val words: List<Word> = words.toList().plusElement(UnknownWord)

    fun match(tokens: TokenList): WordList {
        var remainingTokens = tokens
        val result = mutableListOf<WordInstance>()
        while (remainingTokens.isNotEmpty()) {
            for (word in words) {
                val (consumed, producedWords) = word.consume(remainingTokens) ?: continue
                remainingTokens = remainingTokens.subList(consumed, remainingTokens.size)
                result.addAll(producedWords)
                break
            }
        }
        return result
    }

    companion object DslSupport {
        private val wordsStock = mutableListOf<Word>()

        @DslScopeFunction
        infix fun builtWith(init: DslSupport.() -> Unit): Corpus {
            wordsStock.clear()
            this.init()
            return Corpus(*wordsStock.toTypedArray())
        }

        @DslFunction
        operator fun Word.unaryMinus() {
            wordsStock.add(this)
        }
    }
}
