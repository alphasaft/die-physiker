package nlp.words

import nlp.Consumed
import nlp.WordList
import tokenizing.TokenList
import util.DslInitializer
import util.MayBeInitializedByDsl

class StrictWord(
    override val name: String,
    @MayBeInitializedByDsl private var forms: List<String> = listOf(),
) : Word {

    @DslInitializer("forms")
    infix fun matching(forms: List<String>) = apply { this.forms = forms }
    @DslInitializer("forms")
    infix fun matching(form: String) = apply { this.forms = listOf(form) }

    override fun consume(tokens: TokenList): Pair<Consumed, WordList>? {
        val token = tokens[0]
        return if (forms.any { it == token.value}) {
            1 to listOf(WordInstance(this, name, token.value, token.start))
        } else null
    }
}