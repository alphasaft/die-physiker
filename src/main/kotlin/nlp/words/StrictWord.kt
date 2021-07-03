package nlp.words

import dto.TokenList
import dto.WordInstance
import dto.WordInstanceList
import nlp.Consumed



class StrictWord(
    override val name: String,
    forms: List<String>,
) : Word {
    private val forms = forms.map { it.toLowerCase() }

    override fun consume(tokens: TokenList): Pair<Consumed, WordInstanceList>? {
        val token = tokens[0]
        return if (forms.any { it == token.value}) {
            1 to listOf(WordInstance(this, name, token.value, token.start))
        } else null
    }
}