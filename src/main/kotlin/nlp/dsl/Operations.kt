package nlp.dsl

typealias AnyWordBuilder = WordBuilder<*>
infix fun AnyWordBuilder.then(other: AnyWordBuilder) = WordChainBuilder(listOf(this, other))
infix fun AnyWordBuilder.or(other: AnyWordBuilder) = WordUnionBuilder(listOf(this, other))
