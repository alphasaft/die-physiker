package physics.quantities.expressions

class WhereBindings(
    private val block: Expression,
    private val bindings: Map<String, Expression>,
) : Alias(block.substituteAll(bindings.mapKeys { (k, _) -> Var(k) })) {

    override fun toString(): String {
        return "$block où ${bindings.toList().joinToString(", ") { (v, e) -> "$v: $e" }}"
    }

    override fun differentiate(variable: String): Expression {
        val derivative = super.differentiate(variable)
        val filteredBindings = bindings.filterValues { it in derivative }
        val reversedBindings = filteredBindings.map { (v, expr) -> expr to Var(v) }.toMap()
        return WhereBindings(derivative.substituteAll(reversedBindings), filteredBindings)
    }
}