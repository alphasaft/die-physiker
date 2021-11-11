package physics.computation

import mergeWith
import mergedWith
import physics.ComponentAliasCrashError
import physics.InappropriateKnowledgeException
import physics.VariableNameCrashError
import physics.components.Component
import physics.components.Field
import physics.components.PhysicalSystem
import physics.values.PhysicalValue
import physics.values.castAs


abstract class AbstractPhysicalKnowledge(
    override val name: String,
    protected val requirements: List<ComponentRequirement>,
    outputVariableAndLocation: Pair<String, Location.At>,
) : PhysicalKnowledge() {

    protected val outputVariable: String = outputVariableAndLocation.first
    protected val output: Location.At = outputVariableAndLocation.second
    private val requirementCorrespondingToOutput: ComponentRequirement get() = requirements.single { it.alias == output.alias }

    override fun <T : PhysicalValue<*>> getFieldValue(field: Field<T>, system: PhysicalSystem): Triple<T, AbstractPhysicalKnowledge, String> {
        val fieldOwner = system.findFieldOwner(field)
        val appropriateForm = translateToAppropriateFormInOrderToCompute(field.name, fieldOwner, system)
        val result = appropriateForm.compute(field, system)
        val specificRepresentation = appropriateForm.renderFor(field, system)
        return Triple(result.castAs(field.type), this, specificRepresentation)
    }

    abstract fun translateToAppropriateFormInOrderToCompute(
        fieldName: String,
        owner: Component,
        system: PhysicalSystem
    ): AbstractPhysicalKnowledge

    abstract fun compute(field: Field<*>, system: PhysicalSystem): PhysicalValue<*>

    protected fun fetchVariablesValuesIn(system: PhysicalSystem, outputOwner: Component): Map<String, PhysicalValue<*>> {
        val selectedComponents = selectRequiredComponentsIn(system, outputOwner)
        return requirements
            .map { it.fetchVariablesValuesIn(selectedComponents) }
            .reduce { acc, map -> acc.mergedWith(map) { k, _, _ -> throw VariableNameCrashError(k) } }
    }

    protected fun selectRequiredFieldsIn(system: PhysicalSystem, outputOwner: Component): Map<String, Field<*>> {
        val selectedComponents = selectRequiredComponentsIn(system, outputOwner)
        return requirements
            .map { it.fetchRequiredFieldsIn(selectedComponents) }
            .reduce { acc, map -> acc.mergedWith(map) { _, _ -> throw InternalError() } }
    }

    protected fun selectRequiredComponentsIn(
        system: PhysicalSystem,
        outputOwner: Component
    ): Map<String, Component> {

        val components = mutableMapOf(output.alias to outputOwner)
        val requirements = requirements.sortedByDescending { if (it.selectAll) 0 else 1 }.toMutableList()

        while (requirements.isNotEmpty()) {
            val requirement = requirements.firstOrNull { it.alias in components } ?: requirements.first()
            requirements.remove(requirement)
            components.mergeWith(
                requirement.selectAppropriateComponentsIn(system, components),
                merge = { k, _, _ -> throw ComponentAliasCrashError(k) }
            )
        }

        return components
    }

    protected fun findVariableCorrespondingTo(field: String, owner: Component): String {
        if (field == output.field && owner instanceOf requirementCorrespondingToOutput.type) return outputVariable

        for (requirement in requirements.filter { !it.selectAll && owner instanceOf it.type }) {
            for ((variable, backingField) in requirement.ownedVariables) {
                if (backingField == field) return variable
            }
        }

        throw InappropriateKnowledgeException(this, field, "Can't find variable corresponding to field $field for component of type ${owner.name}")
    }

    protected fun refactorRequirementsToIsolateVariable(variable: String): List<ComponentRequirement> {
        if (variable == outputVariable) return requirements

        val oldOutputRequirement = requirementCorrespondingToOutput
        val newOutputRequirement = requirements.single { variable in it.ownedVariables }
        val matchingField = newOutputRequirement.ownedVariables.getValue(variable)


        return requirements.map { requirement ->
            when {
                requirement === oldOutputRequirement && requirement === newOutputRequirement -> requirement
                    .withRequiredVariable(outputVariable, output.field)
                    .withOptionalField(matchingField)
                requirement === oldOutputRequirement -> requirement.withRequiredVariable(outputVariable, output.field)
                requirement === newOutputRequirement -> requirement.withOptionalField(matchingField)
                else -> requirement
            }
        }
    }
}
