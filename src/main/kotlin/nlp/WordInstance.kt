package nlp


data class WordInstance(
    val model: Word,
    val name: String,
    val value: Any,
)  {
    override fun toString(): String {
        return "WordInstance(name=$name, value=$value)"
    }
}
