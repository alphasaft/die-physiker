package loaders.base

import plus
import java.io.File


@Suppress("unused")
abstract class ParserLogic {

    lateinit var remainingInput: String
    private lateinit var initialInput: String
    private val currentCharNo: Int get() = initialInput.length - remainingInput.length
    private var mostAdvancedFailure: ParsingError? = null
    lateinit var ast: Ast
    private lateinit var astPath: MutableList<String>

    protected open val parasiteChars = "\r"
    protected open val whitespaces = " \t"
    protected open val caseInsensitive = false

    private inner class ParsingAnchor {
        val currentRemainingInput = remainingInput
        val allAstBranches = ast.branches.toSet()
        val oldMostAdvancedFailure = mostAdvancedFailure
    }

    private fun recover(anchor: ParsingAnchor, rollbackMostAdvancedFailure: Boolean = false) {
        remainingInput = anchor.currentRemainingInput
        ast.removeAllBranchesSafeFor(anchor.allAstBranches)
        if (rollbackMostAdvancedFailure) mostAdvancedFailure = anchor.oldMostAdvancedFailure
    }

    private fun fail(message: String, errorCtr: (Pair<Int, Int>, List<String>, String) -> ParsingError = ::SyntaxParsingError): Nothing {
        updateMostAdvancedFailure(errorCtr(charNoAsPosition(), astPath.toList(), message))
        throw mostAdvancedFailure!!
    }

    private fun Pair<Int, Int>.moreAdvancedThan(other: Pair<Int, Int>): Boolean {
        val f1 = first
        val s1 = second
        val f2 = other.first
        val s2 = other.second
        return (f1 > f2 || f1 == f2 && s1 > s2)
    }

    private fun updateMostAdvancedFailure(candidate: ParsingError) {
        val candidatePos = candidate.at
        val mostAdvancedFailurePos = mostAdvancedFailure?.at

        mostAdvancedFailure =
            if (mostAdvancedFailure == null || candidatePos.moreAdvancedThan(mostAdvancedFailurePos!!)) candidate
            else mostAdvancedFailure
    }

    private inline fun <T> ifBlockFails(block: () -> Unit, callback: (SyntaxParsingError) -> T): T? {
        return try {
            block()
            null
        } catch (e: SyntaxParsingError) {
            callback(e)
        }
    }

    private fun charNoAsPosition(charNo: Int = currentCharNo): Pair<Int, Int> {
        val consumedInput = initialInput.take(charNo)
        return consumedInput.count { it == '\n' }+1 to consumedInput.split("\n").last().length+1
    }

    private fun nextToken(): String {
        return when {
            remainingInput == "" -> ""
            remainingInput.first().isLetter() -> remainingInput.takeWhile { it.isLetter() || it.isDigit() }
            remainingInput.first().isDigit() -> remainingInput.takeWhile { it.isDigit() }
            remainingInput.first() == '\n' -> "<end of line>"
            else -> remainingInput.take(1)
        }
    }

    fun parse(file: File) = parse(file.readText())
    fun parse(input: String, asSubParser: Boolean = false): Ast {
        mostAdvancedFailure = null
        initialInput = prepareInput(input)
        remainingInput = initialInput
        ast = prepareAst(input)
        astPath = mutableListOf()

        axiom()

        ast.content = initialInput.dropLast(remainingInput.length)
        if (!asSubParser) {
            eof()
            ast.lock()
        }
        return ast.clean()
    }

    private fun prepareInput(rawInput: String): String {
        return rawInput
            .let { if (caseInsensitive) it.lowercase() else it }
            .filterNot { it in parasiteChars }
            .trim { it.isWhitespace() }
    }

    private fun prepareAst(@Suppress("UNUSED_PARAMETER") input: String): Ast {
        return Ast()
    }

    protected abstract fun axiom()

    private fun eof() {
        if (remainingInput != "") fail("Expected source input to end, got $remainingInput")
    }

    protected fun invokeAsSubParser(parser: ParserLogic, nodeName: String? = null) {
        val offset = charNoAsPosition() + (Pair(-1, -1))
        val path = if (nodeName == null) astPath else astPath + nodeName
        val producedSubAst: Ast

        try {
            producedSubAst = parser.parse(input = remainingInput, asSubParser = true)
        } catch (e: ParsingError) {
            throw e.nestPath(path).withOffset(offset)
        }

        parser.mostAdvancedFailure?.let { updateMostAdvancedFailure(it.nestPath(path).withOffset(offset)) }
        consume(producedSubAst.content!!)
        ast.setNode(path, producedSubAst)
    }

    protected fun consume(what: String, errorMsg: String? = null): String {
        val usedErrorMsg = errorMsg ?: "Expected '${what.replace("\n", "\\n")}', got {input}."

        val caseAdaptedWhat = if (caseInsensitive) what.lowercase() else what
        if (!remainingInput.startsWith(caseAdaptedWhat)) fail(usedErrorMsg.replace("{input}", nextToken()))
        remainingInput = remainingInput.removePrefix(caseAdaptedWhat).dropWhile { it in whitespaces }
        return what
    }

    protected fun consumeSentence(sentence: String) {
        for (word in sentence.split(*whitespaces.toCharArray()).filterNot { it.isEmpty() }) {
            consume(word)
        }
    }

    protected fun consumeRegex(what: String, errorMsg: String? = null): String {
        val usedErrorMsg = errorMsg ?: "Expected something matching regex '${what.replace("\n", "\\n")}', got {input}"
        val regex = Regex("^$what", options = if (caseInsensitive) setOf(RegexOption.IGNORE_CASE) else emptySet())

        val matched = regex.find(remainingInput) ?: fail(usedErrorMsg.replace("{input}", nextToken()))
        remainingInput = remainingInput.drop(matched.value.length).dropWhile { it in whitespaces }
        return matched.value
    }

    protected fun node(name: String?, block: () -> Unit) {
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

    private fun encloseInNode(nodeName: String?, block: () -> Unit): () -> Unit {
        return { node(nodeName) { block() } }
    }

    protected operator fun String.rangeTo(content: String) = content  [ this ]
    protected operator fun String.get(leafName: String): String {
        val fullPath = astPath.plusElement(leafName)
        ast.setNode(fullPath, AstLeaf(this))
        return this
    }

    protected fun optional(nodeName: String? = null, block: () -> Unit) {
        val anchor = ParsingAnchor()
        ifBlockFails(encloseInNode(nodeName, block)) {
            updateMostAdvancedFailure(candidate = it)
            recover(anchor)
        }
    }

    protected inner class ChoiceScope internal constructor(private val content: ChoiceScope.() -> Unit) {
        private var succeeded = false

        fun option(nodeName: String? = null, block: () -> Unit) {
            if (succeeded) return

            val anchor = ParsingAnchor()
            @Suppress("RemoveExplicitTypeArguments")
            ifBlockFails<Nothing>(encloseInNode(nodeName, block)) {
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

    protected fun choice(nodeName: String? = null, block: ChoiceScope.() -> Unit) {
        node(nodeName) {
            ChoiceScope(block).run()
        }
    }

    /**
     * Repeats from [n] to [m] times the given block
     *  If [m] is -1 then the block will be executed [n] times, then until something fails to parse.
     */
    private fun between(n: Int, m: Int, nodeName: String? = null, separator: String = "", block: () -> Unit) {
        require(m == -1 || (m > 0 && m >= n)) { "m should be -1 or greater than 0, and for the latter case greater than or equal to n."}
        require(nodeName == null || '#' in nodeName) { "nodeName for repeatable blocks should be null or include '#'."}

        fun generateBodyForIteration(iterationNo: Int, beginsWithSeparator: Boolean): () -> Unit {
            return {
                if (beginsWithSeparator) consumeRegex(separator)
                node(nodeName?.replace("#", (iterationNo+1).toString())) {
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

    protected fun zeroOrMore(nodeName: String? = null, separator: String = "", block: () -> Unit) {
        return between(0, -1, nodeName, separator, block)
    }

    protected fun oneOrMore(nodeName: String? = null, separator: String = "", block: () -> Unit) {
        return between(1, -1, nodeName, separator, block)
    }

    protected fun atLeast(n: Int, nodeName: String? = null, separator: String = "", block: () -> Unit) {
        return between(n, -1, nodeName, separator, block)
    }

    protected fun atMost(m: Int, nodeName: String? = null, separator: String = "", block: () -> Unit) {
        return between(0, m, nodeName, separator, block)
    }

    protected fun repeat(n: Int, block: () -> Unit) {
        for (i in 0 until n) block()
    }

    protected fun lookahead(string: String, errorMsg: String?) = lookahead { consume(string, errorMsg) }
    protected fun lookahead(block: () -> Unit) {
        val anchor = ParsingAnchor()
        block()
        recover(anchor)
    }

    protected fun negativeLookahead(string: String, errorMsg: String? = null) = negativeLookahead({ consume(string) }, errorMsg)
    protected fun negativeLookahead(block: () -> Unit, errorMsg: String? = null) {
        val anchor = ParsingAnchor()

        @Suppress("RemoveExplicitTypeArguments")
        ifBlockFails<Nothing>(block) {
            recover(anchor, rollbackMostAdvancedFailure = true)
            return
        }

        fail(errorMsg ?: "Negative lookahead succeeded")
    }

    protected inner class AllScope internal constructor(private val separator: String) {
        private val combinations: MutableList<List<() -> Unit>> = mutableListOf(listOf())

        fun block(name: String? = null, block: () -> Unit) {
            val namedBlock = { node(name) { block() } }

            for (combination in combinations.toList()) {
                for (i in combination.indices.plusElement(combination.size)) {
                    combinations.add(combination.subList(0, i) + namedBlock + combination.subList(i, combination.size))
                }
                combinations.remove(combination)
            }
        }

        fun optionalBlock(name: String? = null, block: () -> Unit) {
            val namedBlock = { node(name) { block() } }

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
                        if (combination.isNotEmpty()) {
                            combination.first().invoke()
                            for (block in combination.drop(1)) {
                                consume(separator)
                                block()
                            }
                        }
                    }
                }
            }
        }
    }

    protected fun all(separator: String = "", block: AllScope.() -> Unit) {
        return AllScope(separator).apply(block).run()
    }
}
