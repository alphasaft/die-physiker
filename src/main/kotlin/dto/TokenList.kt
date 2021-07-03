package dto

typealias TokenList = List<Token>
fun TokenList.removeTokens(vararg types: String): TokenList = filterNot { token -> token.type in types }
fun TokenList.removeWhitespaces(): TokenList = removeTokens("whitespace")
