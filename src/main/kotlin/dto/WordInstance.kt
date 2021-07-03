package dto

import nlp.words.Word

data class WordInstance(
    val model: Word,
    val name: String,
    val value: Any,
    val start: Int,
)  {
    override fun toString(): String {
        return "WordInstance(name=$name, value=$value, pos=$start)"
    }
}
