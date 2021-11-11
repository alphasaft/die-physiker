package loaders.base

import java.io.File

abstract class Parser {
    protected lateinit var remainingInput: String
    private lateinit var initialInput: String
    private val currentCharNo: Int get() = initialInput.length - remainingInput.length
    private var mostAdvancedFailure: ParsingError? = null

    private lateinit var ast: Ast
    private lateinit var astPath: MutableList<String>

    protected open val parasiteChars = "\r"
    protected open val whitespaces = " \t"
    protected open val caseInsensitive = false

    protected val string = Regex("\".*?\"")
    protected val identifier = Regex("[a-zA-Zéèçàù_][a-zA-Z0-9éèçàù_]*")
    protected val integer = Regex("\\d+")
    protected val double = Regex("\\d[.,]\\d")

    fun parse(file: File) = parse(file.readText())
    fun parse(input: String, asSubParser: Boolean = false): Ast {
        initialInput = (if (caseInsensitive) input.lowercase() else input).filterNot { it in parasiteChars }.trim { it in " \t\n\r" }
        remainingInput = initialInput
        ast = prepareAst(input)
        astPath = mutableListOf()

        axiom()

        val ast = ast.copy()
        if (asSubParser) consumeRegex("[.\n]*") else { eof() ; ast.lock() }
        return ast.clean()
    }

    private fun prepareAst(input: String): Ast {
        return Ast(input.filterNot { it in parasiteChars })
    }

    protected abstract fun axiom()

    private inner class ParsingAnchor {
        val currentRemainingInput = remainingInput
        val allAstBranches = ast.branches.toSet()
    }

    private fun recover(anchor: ParsingAnchor) {
        remainingInput = anchor.currentRemainingInput
        ast.removeAllBranchesSafeFor(anchor.allAstBranches)
    }

    private fun fail(message: String, errorCtr: (Pair<Int, Int>, String) -> ParsingError = ::SyntaxParsingError): Nothing {
        updateMostAdvancedFailure(errorCtr(charNoAsPosition(), message))
        throw mostAdvancedFailure!!
    }

    private fun updateMostAdvancedFailure(candidate: ParsingError) {
        val candidatePos = candidate.at
        val mostAdvancedFailurePos = mostAdvancedFailure?.at

        mostAdvancedFailure = if (
            mostAdvancedFailure == null
            || candidatePos.first > mostAdvancedFailurePos!!.first
            || ((candidatePos.first == mostAdvancedFailurePos.first) && (candidatePos.second > mostAdvancedFailurePos.second))
        ) candidate else mostAdvancedFailure
    }

    private fun charNoAsPosition(charNo: Int = currentCharNo): Pair<Int, Int> {
        val consumedInput = initialInput.substring(0, charNo)
        return consumedInput.count { it == '\n' }+1 to consumedInput.split("\n").last().length+1
    }

    private inline fun <T> ifBlockFails(block: () -> Unit, callback: (SyntaxParsingError) -> T): T? {
        return try {
            block()
            null
        } catch (e: SyntaxParsingError) {
            callback(e)
        }
    }

    private fun eof() {
        if (remainingInput != "") fail("Expected source input to end, got $remainingInput")
    }

    private fun nextToken(): String {
        return when {
            remainingInput == "" -> ""
            remainingInput.first().isLetter() -> remainingInput.takeWhile { it.isLetter() || it.isDigit() }
            remainingInput.first().isDigit() -> remainingInput.takeWhile { it.isDigit() }
            remainingInput.first() == '\n' -> "\\n"
            else -> remainingInput.take(1)
        }
    }

    protected fun invokeAsSubParser(parser: Parser, groupName: String? = null) {
        val producedSubAst = parser.parse(input = remainingInput, asSubParser = true)
        val path = if (groupName == null) astPath else astPath.plus(groupName)
        consume(producedSubAst.content!!)
        ast.setNode(path, producedSubAst)
    }

    protected fun consume(what: String): String {
        val caseAdaptedWhat = if (caseInsensitive) what.lowercase() else what
        if (!remainingInput.startsWith(caseAdaptedWhat)) fail("Expected '$what', got ${nextToken()}.")
        remainingInput = remainingInput.removePrefix(caseAdaptedWhat).dropWhile { it in whitespaces }
        return what
    }

    protected fun consumeSentence(sentence: String) {
        for (word in sentence.split(*whitespaces.toCharArray()).filterNot { it.isEmpty() }) {
            consume(word)
        }
    }

    protected fun consumeRegex(what: Regex) = consumeRegex(what.pattern)
    protected fun consumeRegex(what: String): String {
        val regex = Regex("^$what", options = if (caseInsensitive) setOf(RegexOption.IGNORE_CASE) else emptySet())
        val matched = regex.find(remainingInput) ?: fail("Expected something matching $what, got ${remainingInput.split(*whitespaces.toCharArray()).first()}")
        remainingInput = remainingInput.drop(matched.value.length).dropWhile { it in whitespaces }
        return matched.value
    }

    protected fun group(name: String? = null, block: () -> Unit) {
        if (name == null) return block()
        val oldRemainingInputStart = currentCharNo
        val container = AstNode().also { ast.setNode(astPath.plusElement(name), it) }
        try {
            astPath.add(name)
            block()
        } finally {
            astPath.removeLast()
        }
        container.content = initialInput.substring(oldRemainingInputStart, currentCharNo).trim()
    }

    private fun encloseInGroup(groupName: String?, block: () -> Unit): () -> Unit {
        return { group(groupName) { block() } }
    }

    protected operator fun String.get(leafName: String): String {
        val fullPath = astPath.plusElement(leafName)
        ast.setNode(fullPath, AstLeaf(this))
        return this
    }

    protected fun optional(groupName: String? = null, block: () -> Unit) {
        val anchor = ParsingAnchor()
        ifBlockFails(encloseInGroup(groupName, block)) {
            updateMostAdvancedFailure(candidate = it)
            recover(anchor)
        }
    }

    protected inner class ChoiceScope internal constructor(private val content: ChoiceScope.() -> Unit) {
        private var succeeded = false

        fun option(groupName: String? = null, block: () -> Unit) {
            if (succeeded) return

            val anchor = ParsingAnchor()
            ifBlockFails(encloseInGroup(groupName, block)) {
                updateMostAdvancedFailure(candidate = it)
                recover(anchor)
                return
            }
            succeeded = true
        }

        fun run() {
            content()
            if (!succeeded) throw mostAdvancedFailure ?: IllegalArgumentException("No options were declared for choices block")
        }
    }

    protected fun choice(groupName: String? = null, block: ChoiceScope.() -> Unit) {
        group(groupName) {
            ChoiceScope(block).run()
        }
    }

    /**
     * Repeats from [n] to [m] times the given block
     *  If [m] is -1 then the block will be executed [n] times, then until something fails to parse.
     */
    protected fun between(n: Int, m: Int, groupName: String? = null, separator: String = "", block: () -> Unit) {
        require(m == -1 || (m > 0 && m >= n)) { "m should be -1 or greater than 0, and for the latter case greater than or equal to n."}
        require(groupName == null || '#' in groupName) { "groupName for repeatable blocks should be null or include '#'."}

        fun generateBodyForIteration(iterationNo: Int, beginsWithSeparator: Boolean): () -> Unit {
            return {
                group(groupName?.replace("#", (iterationNo+1).toString())) {
                    if (beginsWithSeparator) consumeRegex(separator)
                    block()
                }
            }
        }

        var i = 0
        while (i < n) generateBodyForIteration(i, beginsWithSeparator = i++ != 0)()
        while (i != m) {
            val anchor = ParsingAnchor()
            var failed = false
            ifBlockFails(generateBodyForIteration(i, beginsWithSeparator = i++ != 0)) {
                recover(anchor)
                failed = true
            }
            if (failed) break
        }
    }

    protected fun zeroOrMore(groupName: String? = null, separator: String = "", block: () -> Unit) {
        return between(0, -1, groupName, separator, block)
    }

    protected fun oneOrMore(groupName: String? = null, separator: String = "", block: () -> Unit) {
        return between(1, -1, groupName, separator, block)
    }

    protected fun atLeast(n: Int, groupName: String? = null, separator: String = "", block: () -> Unit) {
        return between(n, -1, groupName, separator, block)
    }

    protected fun atMost(m: Int, groupName: String? = null, separator: String = "", block: () -> Unit) {
        return between(0, m, groupName, separator, block)
    }

    protected fun repeat(n: Int, block: () -> Unit) {
        for (i in 0 until n) block()
    }

    protected fun lookahead(block: () -> Unit) {
        val anchor = ParsingAnchor()
        block()
        recover(anchor)
    }

    protected fun negativeLookahead(block: () -> Unit) {
        val anchor = ParsingAnchor()
        ifBlockFails(block) {
            recover(anchor)
            return
        }
        fail("Negative lookahead succeeded")
    }

    protected inner class AllScope internal constructor() {
        private val combinations: MutableList<List<() -> Unit>> = mutableListOf(listOf())

        fun block(name: String? = null, block: () -> Unit) {
            val namedBlock = { group(name) { block() } }

            for (combination in combinations.toList()) {
                for (i in combination.indices.plusElement(combination.size)) {
                    combinations.add(combination.subList(0, i) + namedBlock + combination.subList(i, combination.size))
                }
                combinations.remove(combination)
            }
        }

        fun optionalBlock(name: String? = null, block: () -> Unit) {
            val namedBlock = { group(name) { block() } }

            for (combination in combinations.toList()) {
                for (i in combination.indices.plusElement(combination.size)) {
                    combinations.add(combination.subList(0, i) + namedBlock + combination.subList(i, combination.size))
                }
            }
        }

        internal fun run() {
            choice {
                for (combination in combinations.sortedByDescending { it.size }) {
                    option {
                        for (block in combination) block()
                    }
                }
            }
        }
    }

    protected fun all(block: AllScope.() -> Unit) {
        return AllScope().apply(block).run()
    }
}
