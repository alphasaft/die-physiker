package nlp.dsl

import nlp.words.Word


@WordsDslScope
abstract class WordBuilder<This : WordBuilder<This>> {
    protected var name: String? = null

    infix fun named(name: String): This {
        this.name = name
        @Suppress("UNCHECKED_CAST")
        return (this as This)
    }

    abstract fun build(): Word

    fun checkNameIsProvidedThenReturnIt(): String {
        return name ?: throw IllegalArgumentException("Can't build a word without name")
    }
}
