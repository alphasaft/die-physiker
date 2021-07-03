package nlp.words

import nlp.Consumed
import dto.TokenList
import dto.WordInstanceList

interface Word {
    val name: String
    fun consume(tokens: TokenList): Pair<Consumed, WordInstanceList>?
}
