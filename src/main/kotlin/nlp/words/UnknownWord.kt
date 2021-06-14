package nlp.words

import nlp.Consumed
import nlp.WordList
import tokenizing.TokenList

object UnknownWord : Word {
    override val name = "<unknown>"
    override fun consume(tokens: TokenList): Pair<Consumed, WordList>? {
        val token = tokens[0]
        return 1 to listOf(WordInstance(this, name, token.value, token.start))
    }
}