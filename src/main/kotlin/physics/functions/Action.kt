package physics.functions

import Args
import physics.ComponentsPickerException
import physics.components.Component
import physics.components.Context
import physics.components.ComponentsPicker
import physics.quantities.PValue
import physics.quantities.Quantity


class Action(
    val name: String,
    val specs: ComponentsPicker,
    private val actionImpl: (components: Args<Component>, args: Args<Quantity<*>>) -> Unit
) {
    fun applyOn(component: Component) {
        val context = Context(component)
        val selected = mapOf("THIS" to component)
        val components = specs.pickRequiredComponents(context, selected)
        val variablesValues = specs.pickVariableValues(context, selected)
        actionImpl(components, variablesValues)
    }

    fun applyRepetitivelyOn(component: Component) {
        var previousHashCode: Int
        do {
            previousHashCode = component.hashCode()
            try { applyOn(component) }
            catch (e: ComponentsPickerException) { break }
        } while (previousHashCode != component.hashCode())
    }
}
