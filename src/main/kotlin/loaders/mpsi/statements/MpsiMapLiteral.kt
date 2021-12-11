package loaders.mpsi.statements

class MpsiMapLiteral(private val pairs: Map<Expression, Expression>) : Expression() {
    override fun toString(): String {
        return "{" + pairs.toList().joinToString(", ") { (k, v) -> "$k: $v" } + "}"
    }
}
