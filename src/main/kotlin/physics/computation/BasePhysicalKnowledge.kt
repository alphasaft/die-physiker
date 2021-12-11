package physics.computation

import physics.components.Field
import physics.components.PhysicalSystem
import physics.values.PhysicalValue
import physics.values.castAs
import println

abstract class BasePhysicalKnowledge(val name: String) {
    open fun <T : PhysicalValue<*>> renderFor(field: Field<T>, system: PhysicalSystem): String {
        return toString()
    }

    fun <T : PhysicalValue<*>> fillFieldWithItsValue(field: Field<T>, system: PhysicalSystem) {
        val (value, computedWith, representation) = getFieldValueAndObtentionMethod(field, system)
        field.setContent(value.castAs(field.type), computedWith, representation)
    }

    abstract fun <T : PhysicalValue<*>> getFieldValueAndObtentionMethod(field: Field<T>, system: PhysicalSystem): Triple<T, BasePhysicalKnowledge, String>
}