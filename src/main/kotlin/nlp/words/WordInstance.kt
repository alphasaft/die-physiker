package nlp.words

data class WordInstance(
    val word: Word,
    val name: String,
    val value: Any,
    val start: Int,
)  {
    override fun toString(): String {
        return "WordInstance(name=$name, value=$value, pos=$start)"
    }
}
