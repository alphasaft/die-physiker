package nlp.dsl

fun buildCorpus(builder: CorpusBuilder.() -> Unit) = CorpusBuilder().apply(builder).build()
