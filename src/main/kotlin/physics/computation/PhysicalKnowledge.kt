package physics.computation


import physics.components.Field
import physics.components.PhysicalSystem
import physics.values.PhysicalValue
import physics.values.castAs


abstract class PhysicalKnowledge {
    abstract val name: String
    
    abstract fun <T : PhysicalValue<*>> getFieldValue(field: Field<T>, system: PhysicalSystem): Triple<T, PhysicalKnowledge, String>

    open fun <T : PhysicalValue<*>> renderFor(field: Field<T>, system: PhysicalSystem): String {
        return toString()
    }

    open fun <T : PhysicalValue<*>> fillFieldWithItsValue(field: Field<T>, system: PhysicalSystem) {
        val (value, computedWith, representation) = getFieldValue(field, system)
        field.setContent(value.castAs(field.type), computedWith, representation)
    }
}
