package physics.formulas

import physics.components.Component
import physics.components.Field
import physics.components.PhysicalSystem
import physics.values.PhysicalValue
import physics.values.castAs


class DataMapper(
    val name: String,
    override val requirements: List<Requirement>,
    variables: List<FormulaVariable>,
    private val output: String,
    private val mappers: Map<String, (Map<String, PhysicalValue<*>>) -> PhysicalValue<*>>
) : AbstractPhysicalRelationship() {
    constructor(
        name: String,
        vararg requirements: Requirement,
        variables: List<FormulaVariable>,
        output: String,
        mappers: Map<String, (Map<String, PhysicalValue<*>>) -> PhysicalValue<*>>
    ): this(name, requirements.toList(), variables, output, mappers)

    override val inputVariables: List<FormulaVariable> = variables.filter { it.name != output }
    override val outputVariable: FormulaVariable = variables.single { it.name == output }

    override fun <T : PhysicalValue<*>> computeFieldValue(
        field: Field<T>,
        system: PhysicalSystem
    ): Pair<T, PhysicalRelationship> {
        val fieldOwner = system.fetchFieldOwner(field)
        val appropriateForm = translateToAppropriateFormInOrderToCompute(field.name, fieldOwner)
        if (appropriateForm !== this) return appropriateForm.computeFieldValue(field, system)

        val arguments = generateArgumentsFor(system, outputOwner = fieldOwner)

        return mappers
            .getValue(output)
            .invoke(arguments)
            .castAs(field.type) to this
    }

    private fun translateToAppropriateFormInOrderToCompute(fieldName: String, component: Component): DataMapper {
        if (fieldName == outputField) return this

        val (newOutputRequirement, refactoredRequirements) = refactorRequirementsToFitGivenOutput(fieldName, component)
        val newOutput = inputVariables.first { it.represents(fieldName, newOutputRequirement.name) }.name

        return DataMapper(
            name,
            refactoredRequirements,
            inputVariables + outputVariable,
            newOutput,
            mappers,
        )
    }

    override fun toString(): String {
        return if (inputVariables.size == 1)
            "${inputVariables.single().backingField} --(...)-> ${outputVariable.backingField}"
        else
            "[${inputVariables.joinToString(", ") { it.backingField }}] --(...)-> ${outputVariable.backingField}"
    }
}


