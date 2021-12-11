package loaders.mpsi.statements


internal class MpsiTupleLiteral(private val items: List<Expression>) : Expression() {
    override fun toString(): String {
        return "(${items.joinToString(", ")})"
    }
}
