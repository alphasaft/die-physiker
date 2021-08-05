package physics.computation

import Couple
import mergedWith
import physics.FormulaException
import physics.InappropriateFormula
import physics.VariableNameCrashError
import physics.components.Component
import physics.components.Field
import physics.components.PhysicalSystem
import physics.computation.expressions.Equality
import physics.values.*

class Formula(
    private val obtentionMethod: ObtentionMethod,
    requirements: List<Requirement>,
    output: Couple<String>,
    val expression: Equality,
) : PhysicalRelationship(
    requirements,
    outputVariable = output.first,
    outputOwnerAlias = output.second.split(".").first(),
    outputField = output.second.split(".").last(),
) {

    constructor(
        name: String,
        vararg requirements: Requirement,
        output: Couple<String>,
        expression: Equality,
    ): this(ObtentionMethod.Builtin(name), requirements.toList(), output, expression)


    sealed class ObtentionMethod {
        class Builtin(val name: String) : ObtentionMethod()
        class ByIsolatingVariable(val variable: String, val from: Formula): ObtentionMethod()
        class ByCombiningFormulas(val formulas: List<Formula>): ObtentionMethod()
    }

    override fun <T : PhysicalValue<*>> computeFieldValue(field: Field<T>, system: PhysicalSystem): Pair<T, Formula> {
        val fieldOwner = system.fetchFieldOwner(field)
        val appropriateForm = translateToAppropriateFormInOrderToCompute(field.name, fieldOwner, system)
        val result = appropriateForm.compute(field.name, fieldOwner, system)
        return result.castAs(field.type) to appropriateForm
    }

    fun compute(field: String, of: Component, system: PhysicalSystem): PhysicalDouble {
        val failureCause: FormulaException

        try {
            val correctFormula = translateToAppropriateFormInOrderToCompute(field, of, system)
            if (correctFormula !== this) return correctFormula.compute(field, of, system)

            val arguments = generateArgumentsFor(system, of)
            return expression.compute(arguments.mapValues { (_, v) -> v.toPhysicalDouble() })
        } catch (e: FormulaException) {
            failureCause = e
        }

        throw InappropriateFormula(this, "${of.name}(...).$field", failureCause.message)
    }

    private fun translateToAppropriateFormInOrderToCompute(field: String, of: Component, system: PhysicalSystem): Formula {
        return isolateVariable(field, of).composeWithNestedFormulasOf(system, outputOwner = of)
    }

    private fun isolateVariable(newOutputField: String, newOutputFieldOwner: Component): Formula {
        val modifiedRequirements = refactorRequirementsToFitRequiredOutput(newOutputField, newOutputFieldOwner)
        val variableToIsolate = findVariableCorrespondingTo(newOutputField, newOutputFieldOwner) ?: throw IllegalStateException()
        val modifiedExpression = expression.isolateVariable(variableToIsolate)

        return if (modifiedRequirements == requirements) this else Formula(
            ObtentionMethod.ByIsolatingVariable(variableToIsolate, from = this),
            modifiedRequirements,
            variableToIsolate to "${modifiedRequirements.first { it matches newOutputFieldOwner }.alias}.$newOutputField",
            modifiedExpression
        )
    }

    private fun <V> List<Requirement>.applyAndFlatten(mapper: (Requirement) -> Map<String, V>, errorMessage: (String) -> Throwable): Map<String, V> {
        return this.map(mapper).reduce { acc, new -> acc.mergedWith(new) { k, _, _ -> throw errorMessage(k) } }
    }

    private fun composeWithNestedFormulasOf(system: PhysicalSystem, outputOwner: Component): Formula {
        val selectedComponents = selectAppropriateComponentsIn(system, outputOwner)
        val fieldsCorrespondingToVariables = requirements.applyAndFlatten({ it.fetchFieldsIn(selectedComponents) }) { VariableNameCrashError(it) }

        var result: Formula = this

        for ((variable, field) in fieldsCorrespondingToVariables) {
            if (field.obtainedBy is Formula && Settings.fieldComputationMethod == FieldComputationMethod.ACCURATE) {
                result = result.compose(variable, field.obtainedBy!! as Formula)
            }
        }

        return result
    }

    fun compose(variable: String, formula: Formula): Formula {
        val joiningRequirementInThis = requirements.first { variable in it.ownedVariables }
        val newJoiningRequirement = joiningRequirementInThis.fuseWith(formula.requirementCorrespondingToOutput)


        return Formula(
            ObtentionMethod.ByCombiningFormulas(listOf(this, formula)),
            requirements - formula.requirementCorrespondingToOutputOwner + renamedRequirementsToAvoidNameCrashes,
            inputVariables - variable + variablesRenamedAccordinglyToRequirements + outputVariable,
            expression.composeWith(formula.expression)
        )
    }

    override fun toString(): String {
        return expression.toString()
    }
}










/*

 */