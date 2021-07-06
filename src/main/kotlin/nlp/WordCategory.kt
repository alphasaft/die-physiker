package nlp

import dto.TokenList
import dto.WordInstance
import dto.WordInstanceList
import noop
import toMutableMap


class WordCategory(
    tokenTypesToConverters: List<Pair<String, (String) -> Any>>,
    override val name: String = "<unnamed>",
) : Word {
    private val tokenTypesToConverters = tokenTypesToConverters.ifEmpty { listOf(name to ::noop) }.toMutableMap()

    override fun consume(tokens: TokenList): Pair<Consumed, WordInstanceList>? {
        val token = tokens[0]
        return if (token.type in tokenTypesToConverters.keys) {
            1 to listOf(
                WordInstance(
                this,
                name,
                tokenTypesToConverters.getValue(token.type)(token.value),
                token.start
            )
            )
        } else null
    }
}