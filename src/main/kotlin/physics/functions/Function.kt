package physics.functions

import Args
import physics.components.Component
import physics.components.Context
import physics.components.ComponentsPicker
import physics.quantities.PValue
import physics.quantities.Quantity


class Function(
    val name: String,
    private val specs: ComponentsPicker,
    private val functionImpl: (Args<Component>, Args<Quantity<*>>) -> Component,
) {
    operator fun invoke(context: Context): Component {
        val components = specs.pickRequiredComponents(context)
        val arguments = specs.pickVariableValues(context)
        return functionImpl(components, arguments)
    }
}
