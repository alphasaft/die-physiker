package physics.components

import mergeWith
import mergedWith
import physics.ComponentAliasCrashError
import physics.VariableNameCrashError
import physics.values.PhysicalValue
import println


open class RequirementsHandler(requirements: List<ComponentRequirement>) {
    constructor(vararg requirements: ComponentRequirement): this(requirements.toList())

    protected val requirements = requirements.map { it.withOverlapsForbidden(requirements.map { r -> r.alias }.toSet()) }

    fun selectRequiredComponentsIn(
        system: PhysicalSystem,
        initialComponents: Map<String, Component> = emptyMap(),
    ): Map<String, Component> {
        val components = initialComponents.toMutableMap()
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

    fun getArguments(
        system: PhysicalSystem,
        initialComponents: Map<String, Component> = emptyMap()
    ): Map<String, PhysicalValue<*>> {
        if (requirements.isEmpty()) return emptyMap()

        val selectedComponents = selectRequiredComponentsIn(system, initialComponents)
        return requirements
            .map { it.fetchVariablesValuesIn(selectedComponents) }
            .reduce { acc, map -> acc.mergedWith(map) { k, _, _ -> throw VariableNameCrashError(k) } }
    }

    fun getRequiredFields(system: PhysicalSystem, initialComponents: Map<String, Component> = emptyMap()): Map<String, Field<*>> {
        val selectedComponents = selectRequiredComponentsIn(system, initialComponents)
        return requirements
            .map { it.fetchRequiredFieldsIn(selectedComponents) }
            .reduce { acc, map -> acc.mergedWith(map) { _, _ -> throw InternalError() } }
    }
}
