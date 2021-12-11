package loaders.mpsi.statements

import loaders.mpsi.MpsiType

internal class VariableDeclaration(val variableName: String, val type: MpsiType, val value: Expression?) : VariableDefiner(variableName to type) {
    override fun toString(): String {
        val typeDeclaration = if (type !is MpsiType.Infer) ": $type" else ""
        val valueAsString = if (value != null) " = $value" else ""
        return "var $variableName$typeDeclaration$valueAsString"
    }
}
