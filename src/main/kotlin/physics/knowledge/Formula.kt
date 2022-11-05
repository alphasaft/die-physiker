package physics.knowledge

import Collector
import physics.components.*
import physics.components.ComponentsPickerWithOutput
import physics.quantities.expressions.Equation
import physics.quantities.expressions.Var
import physics.quantities.expressions.equals
import physics.quantities.PValue
import physics.quantities.Quantity
import physics.quantities.toQuantity
import physics.quantities.PDouble
import physics.quantities.expressions.VariableValue


class Formula(
    name: String,
    private val specs: ComponentsPickerWithOutput,
    private val equation: Equation,
) : StandardKnowledge(
    name,
    specs,
    generateMappersFromEquality(equation),
    generateRepresentationDependingOnOutputVariable(equation),
) {

    private companion object Util {
        fun generateMappersFromEquality(equation: Equation): Map<String, Collector<Quantity<*>>> {
            val mappers = mutableMapOf<String, Collector<Quantity<*>>>()
            for (variable in equation.left.allVariables() + equation.right.allVariables()) {
                mappers[variable] = { args -> equation.compute(variable, arguments = args.mapValues { (_, v) -> VariableValue.Single(v.toQuantity<PDouble>()) }) }
            }
            return mappers
        }

        fun generateRepresentationDependingOnOutputVariable(equation: Equation): Map<String, String> {
            val representations = mutableMapOf<String, String>()
            for (variable in equation.left.allVariables() + equation.right.allVariables()) {
                representations[variable] = equation.isolateVariable(variable).toFlatString()
            }
            return representations
        }
    }

    init {
        require(equation.left is Var) { "Expected a variable as the left member of the equality" }
        require(outputVariable == equation.left.name) { "The variable used as output should be the same as the one used in the equality." }
    }

    fun plug(formula: Formula, where: String): Formula = plugImpl(formula, variable = where)
    private fun plugImpl(formula: Formula, variable: String): Formula {
        val fusedSpecs = specs.composedWith(formula.specs, variable)
        val fusedExpressions = equation.composeWith(formula.equation)

        return Formula(
            name = "<Combination of ${this.name} and ${formula.name}>",
            specs = fusedSpecs,
            equation = fusedExpressions,
        )
    }

    override fun <T : PValue<T>> toStringForGivenOutput(field: Field<T>, context: Context): String {
        val outputOwner = context.findFieldOwner(field)
        val fields = specs.pickRequiredFields(context, outputOwner)
        val arguments = specs.pickVariablesValues(context, outputOwner)
        val variablesToNotations = fields
            .mapValues { (_, field) -> field.representation }
            .let { it.mapValues { (variable, notation) -> if (it.values.count { v -> v == notation } > 1) variable else notation } }

        val (_, expression) = equation
        val variablesValues = variablesToNotations.toList().joinToString(", ") { (variable, notation) -> "$notation = ${arguments.getValue(variable)}" }

        return (Var(field.representation) equals expression).toFlatString() + ", où " + variablesValues
    }
}
