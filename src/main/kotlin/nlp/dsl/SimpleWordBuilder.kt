package nlp.dsl

import nlp.words.SimpleWord

class SimpleWordBuilder internal constructor(private val forms: List<String>) : WordBuilder<SimpleWordBuilder>() {
    override fun build(): SimpleWord {
        return SimpleWord(checkNameIsProvidedThenReturnIt(), forms)
    }
}