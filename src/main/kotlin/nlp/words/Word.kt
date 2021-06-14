package nlp.words

import nlp.Consumed
import nlp.WordList
import tokenizing.TokenList

interface Word {
    val name: String
    fun consume(tokens: TokenList): Pair<Consumed, WordList>?
}