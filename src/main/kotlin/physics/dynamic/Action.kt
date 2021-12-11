package physics.dynamic

import physics.Args
import physics.RequirementsException
import physics.components.Component
import physics.components.PhysicalSystem
import physics.components.RequirementsHandler
import physics.values.PhysicalValue


class Action(
    val name: String,
    val requirements: RequirementsHandler,
    private val actionImpl: (components: Args<Component>, args: Args<PhysicalValue<*>>) -> Unit
) {
    fun applyOn(component: Component) {
        val system = PhysicalSystem(component)
        val selected = mapOf("THIS" to component)
        val components = requirements.selectRequiredComponentsIn(system, selected)
        val variablesValues = requirements.getArguments(system, selected)
        actionImpl(components, variablesValues)
    }

    fun applyRepetitivelyOn(component: Component) {
        var previousHashCode: Int
        do {
            previousHashCode = component.hashCode()
            try { applyOn(component) }
            catch (e: RequirementsException) { break }
        } while (previousHashCode != component.hashCode())
    }

}
