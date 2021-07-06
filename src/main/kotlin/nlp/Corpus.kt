package nlp

import dto.TokenList
import dto.WordInstance
import dto.WordInstanceList


class Corpus(vararg words: Word) {
    val words: List<Word> = words.toList().plusElement(UnknownWord)

    fun match(tokens: TokenList): WordInstanceList {
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
}
