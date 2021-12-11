package loaders.mpsi.statements

class ReturnStatement(private val returnedExpression: Expression) : Statement {
    override fun toString(): String {
        return "return $returnedExpression"
    }
}
