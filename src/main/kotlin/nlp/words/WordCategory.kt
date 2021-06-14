package nlp.words

import nlp.Consumed
import nlp.WordList
import tokenizing.TokenList
import util.*


class WordCategory(
    override val name: String,
    vararg tokenTypesToConverters: Pair<String, (String) -> Any>,
) : Word {
    @MayBeMutatedByDsl
    private val tokenTypesToConverters = tokenTypesToConverters.toList().ifEmpty { listOf(name to { v -> v  }) }.toMutableMap()

    @DslScopeFunction
    infix fun definedBy(block: WordCategory.() -> Unit) = apply(block)

    @DslMutator("tokenTypesToConverters")
    operator fun Pair<String, (String) -> Any>.unaryMinus() {
        tokenTypesToConverters[first] = second
    }

    @DslFunction
    infix fun String.thenConvertWith(converter: (String) -> Any): Pair<String, (String) -> Any> {
        return this to converter
    }

    @DslFunction
    fun case(tokenType: String) = tokenType


    override fun consume(tokens: TokenList): Pair<Consumed, WordList>? {
        val token = tokens[0]
        return if (token.type in tokenTypesToConverters.keys) {
            1 to listOf(WordInstance(
                this,
                name,
                tokenTypesToConverters.getValue(token.type)(token.value),
                token.start
            ))
        } else null
    }
}