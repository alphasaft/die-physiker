package nlp

import dto.TokenList
import dto.WordInstance
import dto.WordInstanceList


class StrictWord(
    forms: List<String>,
    override val name: String = "<unnamed>",
) : Word {
    constructor(form: String, name: String = "<unnamed>"): this(listOf(form), name)

    private val forms = forms.map { it.toLowerCase() }

    override fun consume(tokens: TokenList): Pair<Consumed, WordInstanceList>? {
        val token = tokens[0]
        return if (forms.any { it == token.value }) {
            1 to listOf(WordInstance(this, name, token.value, token.start))
        } else null
    }
}