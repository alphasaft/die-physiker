package nlp.words

infix fun Word.then(other: Word) = ComplexWord(listOf(this, other))
infix fun Word.or(other: Word) = WordUnion(listOf(this, other))
