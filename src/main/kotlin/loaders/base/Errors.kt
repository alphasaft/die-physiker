package loaders.base

import plus


abstract class ParsingError(val at: Pair<Int, Int>, val path: List<String>, message: String): Throwable(message, null) {
    override fun toString(): String {
        return if (path.isEmpty()) "At (${at.first}, ${at.second}) : $message"
        else "At (${at.first},${at.second}) : In ${path.joinToString(" > ")} : $message"
    }

    abstract fun withOffset(offset: Pair<Int, Int>): ParsingError
    abstract fun nestPath(path: List<String>): ParsingError
}

class SyntaxParsingError(at: Pair<Int, Int>, path: List<String>, message: String): ParsingError(at, path, message) {
    override fun withOffset(offset: Pair<Int, Int>): ParsingError {
        return SyntaxParsingError(at + offset, path, message!!)
    }

    override fun nestPath(path: List<String>): ParsingError {
        return SyntaxParsingError(at, path + this.path, message!!)
    }
}

class TriggeredParsingError(at: Pair<Int, Int>, path: List<String>, message: String): ParsingError(at, path, message) {
    override fun withOffset(offset: Pair<Int, Int>): ParsingError {
        return TriggeredParsingError(at + offset, path, message!!)
    }

    override fun nestPath(path: List<String>): ParsingError {
        return TriggeredParsingError(at, path + this.path, message!!)
    }
}
