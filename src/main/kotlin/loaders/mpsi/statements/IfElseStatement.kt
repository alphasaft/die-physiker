package loaders.mpsi.statements

internal class IfElseStatement(
    private val condition: Expression,
    private val ifBlock: List<Statement>,
    private val elseBlock: List<Statement>?
) : ScopeDefiner() {

    override fun toString(): String {
        val footer = if (elseBlock != null) """
            else {
                ${elseBlock.joinToString("\n").replace("\n", "\n\t")}
            }
        """.trimMargin() else ""
        return """
            if ($condition) {
                ${ifBlock.joinToString("\n").replace("\n", "\n\t")}
            } $footer
        """.trimIndent()
    }
}