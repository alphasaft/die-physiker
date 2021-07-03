package tokenizing

open class NLPBaseTokenizer(
    vararg tokenModels: TokenModel
) : Tokenizer(*(tokenModels.toSet() union baseTokens()).toTypedArray()) {

    companion object {
        private val lParenthesis = "lParenthesis" matching "\\("
        private val rParenthesis = "rParenthesis" matching "\\)"
        private val whitespace = "whitespace" matching "\\s+"
        private val word = "word" matching "(?i)[a-z][a-z_-]*"
        private val frNumber = "frNumber" matching buildNumberRegex("\\.", ",")
        private val enNumber = "enNumber" matching buildNumberRegex(",", "\\.")
        private val operator = "operator" matching "[-+*/=]"
        private val punctuation = "punctuation" matching "(\\.{3}|[\\.,;:!?])"
        private val apostrophe = "apostrophe" matching "'"

        private fun buildNumberRegex(thousandSeparator: String, decimalSeparator: String): String {
            val digit = "\\d"
            val number = "$digit+"
            val thousandClass = "$digit{3}"
            val incompleteThousandClass = "$digit{1,3}"
            val integerPartWithThousandSeparators = "$incompleteThousandClass($thousandSeparator$thousandClass)*"
            return "($integerPartWithThousandSeparators|$number)($decimalSeparator$number)?"
        }

        fun baseTokens() = setOf(
            lParenthesis,
            rParenthesis,
            whitespace,
            word,
            frNumber,
            enNumber,
            punctuation,
            operator,
        )
    }


}
