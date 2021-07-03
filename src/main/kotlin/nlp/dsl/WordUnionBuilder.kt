package nlp.dsl

import nlp.words.WordUnion

class WordUnionBuilder(private val components: List<WordBuilder<*>>) : WordBuilder<WordUnionBuilder>() {
    override fun build(): WordUnion {
        val name = checkNameIsProvidedThenReturnIt()
        val builtComponents = components.mapIndexed { i, comp -> (comp named "<component ${i+1} of union $name>").build() }
        return WordUnion(builtComponents, name)
    }
}
