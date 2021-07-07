package nlp

class Corpus(vararg words: Word) {
    val words: List<Word> = words.toList().plusElement(UnknownWord)

    fun match(input: String): WordInstanceList {
        var remainingInput = input
        val result = mutableListOf<WordInstance>()
        while (remainingInput.isNotEmpty()) {
            for (word in words) {
                val (consumed, producedWords) = word.consume(remainingInput) ?: continue
                remainingInput = remainingInput.substring(consumed, remainingInput.length)
                result.addAll(producedWords)
                break
            }
        }
        return result
    }
}
