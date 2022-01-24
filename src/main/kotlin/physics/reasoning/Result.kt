package physics.reasoning

import physics.components.Field
import physics.quantities.PValue

sealed interface Result {
    override fun toString(): String

    class FieldValueComputed<T : PValue<T>>(val field: Field<T>) : Result {
        override fun toString(): String {
            return "On trouve $field."  // Value is included in the representation of `field`
        }
    }
}
