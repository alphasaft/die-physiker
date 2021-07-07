package nlp


class WordCategory(
    regexesToConverters: List<Pair<String, (String) -> Any>>,
    override val name: String = "<unnamed>",
) : Word {
    private val regexesToConverters = regexesToConverters.map { Regex("^${it.first}") to it.second }

    override fun consume(input: String): Pair<Consumed, WordInstanceList>? {
        for ((regex, converter) in regexesToConverters) {
            val match = regex.find(input)?.value
            if (match != null) {
                return match.length to listOf(WordInstance(this, name, converter(match)))
            }
        }
        return null
    }
}