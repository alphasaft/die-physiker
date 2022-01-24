package physics.functions

import Args
import physics.components.Context
import physics.components.ComponentsPicker
import physics.quantities.PValue
import physics.quantities.Quantity


class Predicate(
    val name: String,
    private val specs: ComponentsPicker,
    private val predicateImpl: (Args<Quantity<*>>) -> Boolean
) {
    operator fun invoke(context: Context): Boolean {
        val arguments = specs.pickVariableValues(context)
        return predicateImpl(arguments)
    }
}
