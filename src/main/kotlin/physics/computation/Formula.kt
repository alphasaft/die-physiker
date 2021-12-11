package physics.computation

import physics.PhysicalValuesMapper
import physics.components.*
import physics.computation.expressions.Equality
import physics.computation.expressions.Var
import physics.computation.expressions.equal
import physics.values.*


class Formula(
    private val obtentionMethod: ObtentionMethod,
    private val requirements: FlexibleRequirementsHandler,
    private val equality: Equality,
    private val variablesToRenderSpecifically: List<String> = emptyList(),
    private val options: Int = 0,
) : StandardPhysicalKnowledge(
    obtentionMethod.toString(),
    requirements,
    generateMappersFromEquality(equality),
    generateRepresentationDependingOnOutputVariable(equality)
) {

    constructor(
        name: String,
        requirements: FlexibleRequirementsHandler,
        expression: Equality,
        variablesToRenderSpecifically: List<String> = emptyList(),
        options: Int = 0
    ): this(
        ObtentionMethod.Builtin(name),
        requirements,
        expression,
        variablesToRenderSpecifically,
        options,
    )

    private companion object Util {
        fun generateMappersFromEquality(equality: Equality): Map<String, PhysicalValuesMapper> {
            val mappers = mutableMapOf<String, PhysicalValuesMapper>()
            for (variable in equality.left.allVariables() + equality.right.allVariables()) {
                mappers[variable] = { args -> equality.isolateVariable(variable).compute(args.mapValues { (_, v) -> v.toPhysicalDouble() }) }
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

    sealed class ObtentionMethod {
        class Builtin(val name: String) : ObtentionMethod() { override fun toString(): String = name }
        class ByIsolatingVariable(val variable: String, val from: Formula): ObtentionMethod() { override fun toString(): String = from.toString() }
        class ByCombiningFormulas(val formulas: List<Formula>): ObtentionMethod() { override fun toString(): String = "Combinaison de ${formulas.joinToString(", ")}" }
    }

    private val implicit = FormulaOptions.Implicit and options != 0

    init {
        require(equality.left is Var) { "Expected a variable as the left member of the equality" }
        require(outputVariable == equality.left.name) { "The variable used as output should be the same as the one used in the equality." }
    }

    override fun finalizeTranslationToAppropriateFormInOrderToCompute(
        field: Field<*>,
        system: PhysicalSystem
    ): StandardPhysicalKnowledge {
        return composeWithNestedFormulasOf(system.findFieldOwner(field), system)
    }

    private fun composeWithNestedFormulasOf(outputOwner: Component, system: PhysicalSystem): Formula {
        val requiredFields = requirements.getRequiredFields(system, outputOwner)
        var result: Formula = this

        for ((variable, field) in requiredFields) {
            if (field.obtainedBy is Formula && !(field.obtainedBy as Formula).implicit) {
                result = result.plug(field.obtainedBy as Formula, where = variable)
            }
        }

        return result
    }

    private fun plug(formula: Formula, where: String): Formula = plugImpl(formula, variable = where)
    private fun plugImpl(formula: Formula, variable: String): Formula {
        val fusedRequirements = requirements.composedWith(formula.requirements, variable)
        val fusedExpressions = equality.composeWith(formula.equality, variable)
        val combinedFormulasList =
                if (this.obtentionMethod is ObtentionMethod.ByCombiningFormulas) this.obtentionMethod.formulas
                else listOf(this) + if (formula.obtentionMethod is ObtentionMethod.ByCombiningFormulas) formula.obtentionMethod.formulas else listOf(formula)

        return Formula(
            obtentionMethod = ObtentionMethod.ByCombiningFormulas(combinedFormulasList),
            requirements = fusedRequirements,
            equality = fusedExpressions,
            variablesToRenderSpecifically = this.variablesToRenderSpecifically + formula.variablesToRenderSpecifically,
            options = options or formula.options
        )
    }

    override fun <T : PhysicalValue<*>> renderFor(field: Field<T>, system: PhysicalSystem): String {
        val outputOwner = system.findFieldOwner(field)
        val fields = requirements.getRequiredFields(system, outputOwner)

        var variable = equality.left as Var
        var expression = equality.right

        for ((variableName, notation) in fields.mapValues { (_, v) -> v.getNotationFor(system.findFieldOwner(v)) }) {
            if (variableName !in variablesToRenderSpecifically) continue

            if (variableName == variable.name) variable = Var(notation)
            expression = expression.substitute(Var(variableName), Var(notation))
        }
        return (variable equal expression).toFlatString()
    }

    override fun toString(): String {
        return equality.toFlatString()
    }
}
