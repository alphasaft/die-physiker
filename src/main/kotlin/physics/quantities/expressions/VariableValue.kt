package physics.quantities.expressions

import physics.quantities.Quantity
import physics.quantities.doubles.PReal
import kotlin.jvm.Throws

sealed class VariableValue<out T : Quantity<PReal>> {
    class Single<out T : Quantity<PReal>>(val content: T) : VariableValue<T>()

    class Series<out T : Quantity<PReal>>(storage: List<T>) : VariableValue<T>(), List<T> by storage
}
