package nlp

import dto.TokenList
import dto.WordInstance
import dto.WordInstanceList

class SimpleWord(
    forms: List<String>,
    override val name: String = "<unnamed>",
) : Word {
    constructor(form: String, name: String = "<unnamed>"): this(listOf(form), name)

    private val forms: List<String> = forms.map { it.toLowerCase() }

    override fun consume(tokens: TokenList): Pair<Consumed, WordInstanceList>? {
        val token = tokens[0]
        val mostResemblingForm = forms
            .map { it to it.resemblanceTo(token.value) }
            .maxByOrNull { it.second }!!
            .takeIf { it.second.overcomeResemblanceThreshold() }
            ?.first
        return mostResemblingForm?.let { 1 to listOf(WordInstance(this, name, it, token.start)) }
    }
}