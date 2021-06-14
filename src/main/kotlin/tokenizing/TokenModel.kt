package tokenizing


class TokenModel(
    val name: String,
    rawPattern: String,
) {
    private val regex = Regex("^$rawPattern")

    fun match(input: String, startIndex: Int): Token? {
        val matched = regex.find(input)
        if (matched != null) {
            return Token(
                matched.value,
                name,
                startIndex
            )
        }
        return null
    }

    override fun equals(other: Any?) = other is TokenModel && other.hashCode() == hashCode()
    override fun hashCode() = regex.pattern.hashCode()
}


infix fun String.matching(rawPattern: String) = TokenModel(this, rawPattern)
