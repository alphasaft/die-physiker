package nlp


interface Word {
    val name: String
    fun consume(input: String): Pair<Consumed, WordInstanceList>?
}
