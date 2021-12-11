package loaders.mpsi.statements

class VariableAssignment(private val varName: String, private val varValue: Expression) : Statement {

    override fun toString(): String {
        return "$varName = $varValue"
    }
}