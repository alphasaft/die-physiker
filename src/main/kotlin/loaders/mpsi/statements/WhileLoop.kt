package loaders.mpsi.statements

internal class WhileLoop(
    private val condition: Expression,
    private val body: List<Statement>,
) : ScopeDefiner() {
    override fun toString(): String {
        return "while ($condition) {\n" +
                "    " + body.joinToString("\n").replace("\n", "\n\t") +
                "\n}"
    }
}
