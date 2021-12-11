package physics.dynamic

import physics.Args
import physics.components.Component
import physics.components.PhysicalSystem
import physics.components.RequirementsHandler
import physics.values.PhysicalValue


class Function(
    val name: String,
    private val requirements: RequirementsHandler,
    private val functionImpl: (Args<Component>, Args<PhysicalValue<*>>) -> Component,
) {
    operator fun invoke(system: PhysicalSystem): Component {
        val components = requirements.selectRequiredComponentsIn(system)
        val arguments = requirements.getArguments(system)
        return functionImpl(components, arguments)
    }
}
