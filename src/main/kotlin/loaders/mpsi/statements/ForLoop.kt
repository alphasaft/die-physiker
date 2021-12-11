package loaders.mpsi.statements

import loaders.mpsi.MpsiType


internal class ForLoop(
    private val iteratingVariable: Pair<String, MpsiType>,
    private val iteratedValue: Expression,
    private val body: List<Statement>
) : ScopeDefiner(iteratingVariable) {
    private val iteratingVariableName: String = iteratingVariable.first
    private val iteratingVariableType: MpsiType = iteratingVariable.second

    override fun toString(): String {
        val header = if (iteratingVariableType is MpsiType.Infer) "for ($iteratingVariableName in $iteratedValue)"
        else "for ($iteratingVariableName: $iteratingVariableType in $iteratedValue)"
        return """
            $header {
                ${body.joinToString("\n").replace("\n", "\\n")}
            }
        """.trimIndent()
    }
}
