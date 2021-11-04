package physics.computation

import mergeWith
import mergedWith
import physics.ComponentAliasCrashError
import physics.VariableNameCrashError
import physics.components.Component
import physics.components.Field
import physics.components.PhysicalSystem
import physics.computation.formulas.Formula
import physics.values.PhysicalValue
import physics.values.castAs


abstract class AbstractPhysicalKnowledge(
    protected val requirements: List<ComponentRequirement>,
    output: Pair<String, String>,
) : PhysicalKnowledge {
    protected val outputVariable: String = output.first
    protected val outputOwnerAlias: String = output.second.split(".").first()
    protected val outputField: String = output.second.split(".").last()
    private val requirementCorrespondingToOutput: ComponentRequirement get() = requirements.single { it.alias == outputOwnerAlias }

    override fun <T : PhysicalValue<*>> getFieldValue(field: Field<T>, system: PhysicalSystem): Pair<T, AbstractPhysicalKnowledge> {
        val fieldOwner = system.findFieldOwner(field)
        val appropriateForm = translateToAppropriateFormInOrderToCompute(field.name, fieldOwner, system)
        val result = appropriateForm.compute(field, system)
        return result.castAs(field.type) to appropriateForm
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

    protected fun selectRequiredComponentsIn(
        system: PhysicalSystem,
        outputOwner: Component
    ): Map<String, Component> {
        val components = mutableMapOf(outputOwnerAlias to outputOwner)
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
        for (requirement in requirements.filter { !it.selectAll && owner instanceOf it.type }) {
            for ((variable, backingField) in requirement.ownedVariables) {
                if (backingField == field) return variable
            }
        }
        return outputVariable
    }

    protected fun refactorRequirementsToIsolateVariable(variable: String): List<ComponentRequirement> {
        if (variable == outputVariable) return requirements

        val oldOutputRequirement = requirementCorrespondingToOutput
        val newOutputRequirement = requirements.single { variable in it.ownedVariables }
        val matchingField = newOutputRequirement.ownedVariables.getValue(variable)


        return requirements.map { requirement ->
            when {
                requirement === oldOutputRequirement && requirement === newOutputRequirement -> requirement
                    .withRequiredVariable(outputVariable, outputField)
                    .withOptionalField(matchingField)
                requirement === oldOutputRequirement -> requirement.withRequiredVariable(outputVariable, outputField)
                requirement === newOutputRequirement -> requirement.withOptionalField(matchingField)
                else -> requirement
            }
        }
    }
}
