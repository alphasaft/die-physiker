package physics.components

import mergeWith
import mergedWith
import physics.ComponentsPickerException
import physics.quantities.PValue
import physics.quantities.Quantity


open class ComponentsPicker(specs: List<ComponentSpec>) {
    constructor(vararg specs: ComponentSpec): this(specs.toList())

    protected val specs = specs.map { it.withOverlapsForbidden(specs.map { s -> s.alias }.toSet()) }

    fun pickRequiredComponents(
        context: Context,
        initialComponents: Map<String, Component> = emptyMap(),
    ): Map<String, Component> {
        val components = initialComponents.toMutableMap()
        val specs = specs.toMutableList()

        while (specs.isNotEmpty()) {
            val spec = specs.firstOrNull { it.alias in components } ?: specs.first()
            specs.remove(spec)
            components.mergeWith(
                spec.fetchAppropriateComponentsIn(context, components),
                merge = { k, _, _ -> throw ComponentsPickerException("Two or more components were registered under the alias $k.") }
            )
        }

        return components
    }
    
    fun pickRequiredFields(context: Context, initialComponents: Map<String, Component> = emptyMap()): Map<String, Field<*>> {
        val selectedComponents = pickRequiredComponents(context, initialComponents)
        return specs
            .map { it.fetchRequiredFieldsIn(selectedComponents) }
            .reduce { acc, map -> acc.mergedWith(map) { _, _ -> throw InternalError() } }
    }

    fun pickVariableValues(
        context: Context,
        initialComponents: Map<String, Component> = emptyMap()
    ): Map<String, Quantity<*>> {
        if (specs.isEmpty()) return emptyMap()

        val selectedComponents = pickRequiredComponents(context, initialComponents)
        return specs
            .map { it.fetchVariablesValuesIn(selectedComponents) }
            .reduce { acc, map -> acc.mergedWith(map) { k, _, _ -> throw ComponentsPickerException("Got two different values for variable $k.") } }
    }
}
