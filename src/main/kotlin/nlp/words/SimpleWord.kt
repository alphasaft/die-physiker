package nlp.words

import dto.TokenList
import dto.WordInstance
import dto.WordInstanceList
import nlp.Consumed
import nlp.overcomeResemblanceThreshold
import nlp.resemblanceTo


class SimpleWord(
    override val name: String,
    forms: List<String>
) : Word {
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
