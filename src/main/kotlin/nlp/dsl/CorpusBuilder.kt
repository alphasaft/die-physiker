package nlp.dsl

import nlp.Corpus

@WordsDslScope
class CorpusBuilder {
    val words = mutableListOf<WordBuilder<*>>()

    operator fun <T : WordBuilder<*>> T.unaryMinus(): T {
        this@CorpusBuilder.words.add(this)
        return this
    }

    fun word(vararg forms: String) = SimpleWordBuilder(forms.toList())
    fun strictWord(vararg forms: String) = StrictWordBuilder(forms.toList())
    fun wordCategory() = WordCategoryBuilder()
    fun wordCategory(init: WordCategoryBuilder.() -> Unit) = WordCategoryBuilder().apply(init)

    fun build(): Corpus = Corpus(*words.map { it.build() }.reversed().toTypedArray())
}
