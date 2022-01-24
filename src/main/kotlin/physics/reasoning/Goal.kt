package physics.reasoning

import physics.components.Field
import physics.quantities.PValue

sealed interface Goal {
    fun isSolvedBy(result: Result): Boolean
    override fun toString(): String

    class GetFieldValue<T : PValue<T>>(val field: Field<T>) : Goal {
        override fun isSolvedBy(result: Result): Boolean {
            return result is Result.FieldValueComputed<*> && result.field === field
        }

        override fun toString(): String {
            return "On cherche ${field.getNotation()}."
        }
    }
}
