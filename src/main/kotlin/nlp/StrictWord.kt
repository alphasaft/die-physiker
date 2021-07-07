package nlp


class StrictWord(
    private val forms: List<String>,
    override val name: String = "<unnamed>",
) : Word {
    constructor(form: String, name: String = "<unnamed>"): this(listOf(form), name)

    override fun consume(input: String): Pair<Consumed, WordInstanceList>? {
        val selected = forms.find { input.startsWith(it) }
        return if (selected != null) selected.length to listOf(WordInstance(this, name, selected))
        else null
    }
}