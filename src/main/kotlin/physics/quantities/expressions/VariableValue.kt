package physics.quantities.expressions

import physics.quantities.Quantity
import physics.quantities.PReal

sealed class VariableValue<out T : Quantity<PReal>> {
    class Single<out T : Quantity<PReal>>(val content: T) : VariableValue<T>()

    class Array<out T : Quantity<PReal>>(storage: List<T>) : VariableValue<T>(), List<T> by storage
}
