package nlp.dsl

import dto.WordInstanceList
import nlp.words.WordChain

class WordChainBuilder(private val components: List<WordBuilder<*>>) : WordBuilder<WordChainBuilder>() {
    private var concatenate: WordChain.(WordInstanceList) -> WordInstanceList = WordChain.Util::defaultConcatenate

    infix fun concatenateWith(concatenate: WordChain.(WordInstanceList) -> WordInstanceList) {
        this.concatenate = concatenate
    }

    override fun build(): WordChain {
        val name = checkNameIsProvidedThenReturnIt()
        val builtComponents = components.mapIndexed { i, comp -> (comp named "<component ${i+1} of word chain $name>").build() }
        return WordChain(builtComponents, name, concatenate)
    }
}
