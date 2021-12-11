package loaders.mpsi.statements

class MpsiListLiteral(private val items: List<Expression>) : Expression() {
    override fun toString(): String {
        return items.toString()
    }
}
