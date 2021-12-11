package loaders.mpsi.statements

import loaders.mpsi.statements.Expression

class VariableAccess(private val variableName: String) : Expression() {
    override fun toString(): String {
        return variableName
    }
}
