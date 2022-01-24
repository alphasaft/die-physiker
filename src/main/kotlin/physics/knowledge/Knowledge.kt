package physics.knowledge

import physics.components.Field
import physics.components.Context
import physics.quantities.PValue
import physics.quantities.Quantity


interface Knowledge {
    val name: String
    fun <T : PValue<T>> getFieldValue(field: Field<T>, context: Context): Quantity<T>
    fun <T : PValue<T>> toStringForGivenOutput(field: Field<T>, context: Context): String = toString()
}
