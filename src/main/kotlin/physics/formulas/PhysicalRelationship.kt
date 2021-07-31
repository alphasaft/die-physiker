package physics.formulas

import mergeWith
import physics.InappropriateFormula
import physics.components.Field
import physics.components.PhysicalSystem
import physics.components.Component
import physics.values.PhysicalValue
import physics.values.castAs


interface PhysicalRelationship {
    fun <T : PhysicalValue<*>> computeFieldValue(field: Field<T>, system: PhysicalSystem): Pair<T, PhysicalRelationship>

    fun <T : PhysicalValue<*>> fillFieldWithItsValue(field: Field<T>, system: PhysicalSystem) {
        val (value, computedWith) = computeFieldValue(field, system)
        field.setContent(value.castAs(field.type), computedWith)
    }
}


abstract class AbstractPhysicalRelationship : PhysicalRelationship {
    protected abstract val requirements: List<Requirement>
    protected abstract val inputVariables: List<FormulaVariable>
    protected abstract val outputVariable: FormulaVariable
    private val outputOwnerName: String get() = outputVariable.owner
    protected val outputField: String get() = outputVariable.backingField
    protected val requirementCorrespondingToOutputOwner get() = requirements.single { it.name == outputOwnerName }

    protected fun generateArgumentsFor(system: PhysicalSystem, outputOwner: Component): Map<String, PhysicalValue<*>> {
        val selectedComponents = selectAppropriateComponentsIn(system, outputOwner)
        val fields = associateVariablesToFields(selectedComponents)
        return fields.map { (k, v) -> k.name to v.getContent() }.toMap()
    }

    protected fun selectAppropriateComponentsIn(system: PhysicalSystem, outputOwner: Component): Map<String, Component> {
        val components = mutableMapOf(outputOwnerName to outputOwner)
        val requirements = requirements.toMutableList()

        while (requirements.isNotEmpty()) {
            val requirement = requirements.firstOrNull { it.name in components } ?: requirements.first()
            requirements.remove(requirement)
            components.mergeWith(requirement.selectIn(system, components), merge = { _, _ -> throw IllegalArgumentException() })
        }
        return components
    }

    protected fun associateVariablesToFields(selectedComponents: Map<String, Component>): Map<FormulaVariable, Field<*>> {
        return inputVariables.associateWith { it.findCorrespondingFieldIn(selectedComponents) }
    }

    protected fun refactorRequirementsToFitGivenOutput(newOutputField: String, newOutputFieldOwner: Component): Pair<Requirement, List<Requirement>> {
        val requirementsCorrespondingToFieldOwner = requirements.filter { it.withOptionalField(newOutputField) matchesSingle newOutputFieldOwner }
        val suitableRequirementForRefactoring = requirementsCorrespondingToFieldOwner.find { it.requiresField(newOutputField) }

        if (requirementsCorrespondingToFieldOwner.any { it === requirementCorrespondingToOutputOwner } && newOutputField == outputField)
            return requirementCorrespondingToOutputOwner to requirements
        else (suitableRequirementForRefactoring ?: throw InappropriateFormula(this, "${newOutputFieldOwner.name}(...).$newOutputField"))

        return suitableRequirementForRefactoring to if (suitableRequirementForRefactoring == requirementCorrespondingToOutputOwner) {
            (requirements
                    - suitableRequirementForRefactoring
                    + suitableRequirementForRefactoring.withOptionalField(newOutputField).withRequiredField(outputField))
        } else {
            (requirements
                    - suitableRequirementForRefactoring - requirementCorrespondingToOutputOwner
                    + suitableRequirementForRefactoring.withOptionalField(newOutputField) + requirementCorrespondingToOutputOwner.withRequiredField(outputField))
        }
    }
}