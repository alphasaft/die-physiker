package physics.knowledge

import Collector
import physics.components.*
import physics.components.ComponentsPickerWithOutput
import physics.quantities.expressions.Equality
import physics.quantities.expressions.Var
import physics.quantities.expressions.equal
import physics.quantities.PValue
import physics.quantities.Quantity
import physics.quantities.castAs
import physics.quantities.doubles.PReal
import physics.quantities.expressions.VariableValue


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
        fun generateMappersFromEquality(equality: Equality): Map<String, Collector<Quantity<*>>> {
            val mappers = mutableMapOf<String, Collector<Quantity<*>>>()
            for (variable in equality.left.allVariables() + equality.right.allVariables()) {
                @Suppress("RemoveExplicitTypeArguments")
                mappers[variable] = { args -> equality.compute(variable, arguments = args.mapValues { (_, v) -> VariableValue.Single(v.castAs<PReal>()) }) }
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

        val expression = equality.right
        val variablesValues = variablesToNotations.toList().joinToString(", ") { (variable, notation) -> "$notation = ${arguments.getValue(variable)}" }

        return (Var(field.getNotation()) equal expression).toFlatString() + ", où " + variablesValues
    }
}
