package physics.quantities.expressions

import physics.quantities.Quantity
import physics.quantities.PDouble

sealed class VariableValue<out T : Quantity<PDouble>> {
    class Single<out T : Quantity<PDouble>>(val content: T) : VariableValue<T>()

    class Array<out T : Quantity<PDouble>>(storage: List<T>) : VariableValue<T>(), List<T> by storage
}
