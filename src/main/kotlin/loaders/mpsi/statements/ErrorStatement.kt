package loaders.mpsi.statements

internal class ErrorStatement(val message: String) : Statement {
    override fun toString(): String {
        return "error $message"
    }
}
