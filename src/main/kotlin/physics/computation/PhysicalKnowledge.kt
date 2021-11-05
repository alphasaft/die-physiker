package physics.computation


import physics.components.Field
import physics.components.PhysicalSystem
import physics.values.PhysicalValue
import physics.values.castAs


interface PhysicalKnowledge {
    val name: String

    fun <T : PhysicalValue<*>> getFieldValue(field: Field<T>, system: PhysicalSystem): Pair<T, PhysicalKnowledge>

    fun <T : PhysicalValue<*>> fillFieldWithItsValue(field: Field<T>, system: PhysicalSystem) {
        val (value, computedWith) = getFieldValue(field, system)
        field.setContent(value.castAs(field.type), computedWith)
    }
}
