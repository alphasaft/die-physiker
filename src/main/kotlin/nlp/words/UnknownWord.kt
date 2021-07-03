package nlp.words

import dto.TokenList
import dto.WordInstance
import dto.WordInstanceList
import nlp.Consumed

object UnknownWord : Word {
    override val name = "<unknown>"
    override fun consume(tokens: TokenList): Pair<Consumed, WordInstanceList> {
        val token = tokens[0]
        return 1 to listOf(WordInstance(this, name, token.value, token.start))
    }
}
