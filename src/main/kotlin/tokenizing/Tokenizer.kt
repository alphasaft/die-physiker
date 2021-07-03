package tokenizing

import util.normalize
import dto.Token
import dto.TokenList


open class Tokenizer(private vararg val tokenModels: TokenModel) {
    fun tokenize(input: String): TokenList {
        var remainingInput = input.normalize()
        val tokens = mutableListOf<Token>()
        do {
            val token = createTokenFrom(tokens.lastOrNull()?.end ?: 1, remainingInput)
            tokens.add(token)
            remainingInput = remainingInput.substring(token.length)
        } while (remainingInput.isNotEmpty())
        return tokens
    }

    private fun createTokenFrom(startIndex: Int, input: String): Token {
        val candidates = mutableListOf<Token>()
        for (tokenModel in tokenModels) {
            tokenModel.match(input, startIndex)?.let { candidates.add(it) }
        }
        return candidates.maxByOrNull { it.length } ?: unknownToken(input[0], startIndex)
    }

    companion object {
        private fun unknownToken(content: Char, pos: Int) = Token(content.toString(), "<unknown>", pos)
    }
}
