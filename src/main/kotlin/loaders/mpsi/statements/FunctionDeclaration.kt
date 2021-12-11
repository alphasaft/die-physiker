package loaders.mpsi.statements

import loaders.mpsi.FunctionSignature

internal class FunctionDeclaration(
    private val functionName: String,
    private val signature: FunctionSignature,
    private val body: List<Statement>,
) : Statement {
    override fun toString(): String {
        return "function $functionName$signature {\n" +
               "    ${body.joinToString("\n").replace("\n", "\n\t")}" +
               "\n}"
    }
}