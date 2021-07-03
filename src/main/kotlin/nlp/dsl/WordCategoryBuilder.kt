package nlp.dsl

import nlp.words.WordCategory

class WordCategoryBuilder : WordBuilder<WordCategoryBuilder>() {
    private val tokenNamesToConverters = mutableListOf<Pair<String, (String) -> Any>>()

    fun definedBy(block: WordCategoryBuilder.() -> Unit) {
        this.block()
    }

    inner class UnInitializedTokenToConverterObject internal constructor(private val tokenName: String) {
        infix fun convertedWith(converter: (String) -> Any) {
            this@WordCategoryBuilder.tokenNamesToConverters.add(tokenName to converter)
        }
    }

    operator fun String.unaryMinus() = UnInitializedTokenToConverterObject(this)

    override fun build(): WordCategory {
        return WordCategory(checkNameIsProvidedThenReturnIt(), tokenNamesToConverters)
    }
}