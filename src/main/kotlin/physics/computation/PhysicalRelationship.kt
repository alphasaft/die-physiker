package physics.computation

import mergeWith
import mergedWith
import physics.ComponentAliasCrashError
import physics.VariableNameCrashError
import physics.components.Field
import physics.components.PhysicalSystem
import physics.components.Component
import physics.values.PhysicalValue
import physics.values.castAs




abstract class PhysicalRelationship(
    protected val requirements: List<Requirement>,
    private val outputOwnerAlias: String,
    private val outputField: String,
    private val outputVariable: String,
) {
    protected val requirementCorrespondingToOutput: Requirement get() = requirements.single { it.alias == outputOwnerAlias }

    abstract fun <T : PhysicalValue<*>> computeFieldValue(field: Field<T>, system: PhysicalSystem): Pair<T, PhysicalRelationship>

    fun <T : PhysicalValue<*>> fillFieldWithItsValue(field: Field<T>, system: PhysicalSystem) {
        val (value, computedWith) = computeFieldValue(field, system)
        field.setContent(value.castAs(field.type), computedWith)
    }

    protected fun generateArgumentsFor(system: PhysicalSystem, outputOwner: Component): Map<String, PhysicalValue<*>> {
        val selectedComponents = selectAppropriateComponentsIn(system, outputOwner)
        return requirements
            .map { it.fetchVariablesIn(selectedComponents) }
            .reduce { acc, map -> acc.mergedWith(map) { _, _ -> throw VariableNameCrashError(acc.keys.first { it in map }) } }
    }

    protected fun selectAppropriateComponentsIn(
        system: PhysicalSystem,
        outputOwner: Component
    ): Map<String, Component> {
        val components = mutableMapOf(outputOwnerAlias to outputOwner)
        val requirements = requirements.toMutableList()

        while (requirements.isNotEmpty()) {
            val requirement = requirements.firstOrNull { it.alias in components } ?: requirements.first()
            requirements.remove(requirement)
            components.mergeWith(
                requirement.selectAppropriateComponentsIn(system, components),
                merge = { _, _ -> throw ComponentAliasCrashError(requirement.alias) })
        }

        return components
    }

    protected fun findVariableCorrespondingTo(field: String, owner: Component): String? {
        for (requirement in requirements.filter { owner instanceOf it.type }) {
            for ((variable, backingField) in requirement.ownedVariables) {
                if (backingField == field) return variable
            }
        }
        return null
    }

    protected fun refactorRequirementsToFitRequiredOutput(newOutputField: String, newOutputOwner: Component): List<Requirement> {
        val oldOutputRequirement = requirements.single { it.alias == outputOwnerAlias }
        val requirementSuitableToRefactorAsOutputRequirement = requirements.first { !it.selectAll && it matches newOutputOwner && it requiresField newOutputField }
        return requirements.map { when {
            it === oldOutputRequirement -> it.withRequiredVariable(outputVariable, outputField)
            it === requirementSuitableToRefactorAsOutputRequirement -> it.withOptionalField(newOutputField)
            else -> it
        } }
    }
}
