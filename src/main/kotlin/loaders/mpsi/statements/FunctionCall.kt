package loaders.mpsi.statements

internal class FunctionCall(private val functionName: String, private val arguments: List<Expression>) : Expression() {
    override fun toString(): String {
        return "$functionName(${arguments.joinToString(", ")})"
    }
}
