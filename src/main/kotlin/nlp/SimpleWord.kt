package nlp

import normalize
import java.util.*

class SimpleWord(
    forms: List<String>,
    override val name: String = "<unnamed>",
) : Word {
    constructor(form: String, name: String = "<unnamed>"): this(listOf(form), name)

    private val forms: List<String> = forms.map { it.lowercase(Locale.getDefault()) }

    override fun consume(input: String): Pair<Consumed, WordInstanceList>? {
        val mostResemblingForm = forms
            .map { it to it.resemblanceTo(input.substring(0, it.length).normalize()) }
            .maxByOrNull { it.second }!!
            .takeIf { it.second.overcomeResemblanceThreshold() }
            ?.first
        return mostResemblingForm?.let { 1 to listOf(WordInstance(this, name, it)) }
    }
}