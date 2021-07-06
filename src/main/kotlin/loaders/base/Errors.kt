package loaders.base


abstract class ParsingError(val at: Pair<Int, Int>, message: String): Throwable(message, null) {
    override fun toString(): String {
        return "At (${at.first},${at.second}) : $message"
    }
}

class SyntaxParsingError(at: Pair<Int, Int>, message: String): ParsingError(at, message)
class TriggeredParsingError(at: Pair<Int, Int>, message: String): ParsingError(at, message)
