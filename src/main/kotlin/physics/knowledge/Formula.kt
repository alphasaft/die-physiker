package physics.knowledge

import physics.QuantityMapper
import physics.components.*
import physics.components.ComponentsPickerWithOutput
import physics.values.equalities.Equality
import physics.values.equalities.Var
import physics.values.equalities.equal
import physics.quantities.PValue
import physics.quantities.castAs
import physics.quantities.doubles.PReal


class Formula(
    name: String,
    private val specs: ComponentsPickerWithOutput,
    private val equality: Equality,
) : StandardKnowledge(
    name,
    specs,
    generateMappersFromEquality(equality),
    generateRepresentationDependingOnOutputVariable(equality),
) {

    private companion object Util {
        fun generateMappersFromEquality(equality: Equality): Map<String, QuantityMapper> {
            val mappers = mutableMapOf<String, QuantityMapper>()
            for (variable in equality.left.allVariables() + equality.right.allVariables()) {
                mappers[variable] = { args -> equality.compute(variable, arguments = args.mapValues { (_, v) -> v.castAs<PReal>() }) }
            }
            return mappers
        }

        fun generateRepresentationDependingOnOutputVariable(equality: Equality): Map<String, String> {
            val representations = mutableMapOf<String, String>()
            for (variable in equality.left.allVariables() + equality.right.allVariables()) {
                representations[variable] = equality.isolateVariable(variable).toFlatString()
            }
            return representations
        }
    }

    init {
        require(equality.left is Var) { "Expected a variable as the left member of the equality" }
        require(outputVariable == equality.left.name) { "The variable used as output should be the same as the one used in the equality." }
    }

    fun plug(formula: Formula, where: String): Formula = plugImpl(formula, variable = where)
    private fun plugImpl(formula: Formula, variable: String): Formula {
        val fusedSpecs = specs.composedWith(formula.specs, variable)
        val fusedExpressions = equality.composeWith(formula.equality, variable)

        return Formula(
            name = "<Combination of ${this.name} and ${formula.name}>",
            specs = fusedSpecs,
            equality = fusedExpressions,
        )
    }

    override fun <T : PValue<T>> toStringForGivenOutput(field: Field<T>, context: Context): String {
        val outputOwner = context.findFieldOwner(field)
        val fields = specs.pickRequiredFields(context, outputOwner)
        val arguments = specs.pickVariablesValues(context, outputOwner)
        val variablesToNotations = fields
            .mapValues { (_, field) -> field.getNotation() }
            .let { it.mapValues { (variable, notation) -> if (it.values.count { v -> v == notation } > 1) variable else notation } }

        val expression = equality.right.transformBy(variablesToNotations)
        val variablesValues = variablesToNotations.toList().joinToString(", ") { (variable, notation) -> "$notation = ${arguments.getValue(variable)}" }

        return (Var(field.getNotation()) equal expression).toFlatString() + ", où " + variablesValues
    }
}
