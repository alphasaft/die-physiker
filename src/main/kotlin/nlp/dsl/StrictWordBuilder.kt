package nlp.dsl

import nlp.words.StrictWord
import nlp.words.Word

class StrictWordBuilder(private val forms: List<String>) : WordBuilder<StrictWordBuilder>() {
    override fun build(): StrictWord {
        return StrictWord(checkNameIsProvidedThenReturnIt(), forms)
    }
}