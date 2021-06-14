package nlp.words

import nlp.Consumed
import nlp.WordList
import nlp.overcomeResemblanceThreshold
import nlp.resemblanceTo
import tokenizing.TokenList
import util.MayBeInitializedByDsl
import util.DslInitializer

class SimpleWord(
    override val name: String,
    @MayBeInitializedByDsl private var forms: List<String> = listOf()
) : Word {

    @DslInitializer("forms")
    infix fun matching(forms: List<String>): SimpleWord = apply { this.forms = forms }
    @DslInitializer("forms")
    infix fun matching(form: String): SimpleWord = apply { this.forms = listOf(form) }

    override fun consume(tokens: TokenList): Pair<Consumed, WordList>? {
        val token = tokens[0]
        val mostResemblingForm = forms
            .map { it to it.resemblanceTo(token.value) }
            .maxByOrNull { it.second }!!
            .takeIf { it.second.overcomeResemblanceThreshold() }
            ?.first
        return mostResemblingForm?.let { 1 to listOf(WordInstance(this, name, it, token.start)) }
    }
}
