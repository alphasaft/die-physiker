package physics.components

import physics.values.PhysicalValue
import physics.values.castAs

class ComponentModifier(component: Component) {
    val component = component.copy()

    class SubcomponentGroupModifier(groupContent: List<Component>) {
        private val groupContent = groupContent.mapTo(mutableListOf()) { it.copy() }

        operator fun Component.unaryMinus() { groupContent.remove(this) }
        operator fun Component.unaryPlus() { groupContent.add(this) }
        fun build() = groupContent
    }

    class FieldModifier<T : PhysicalValue<*>>(private val field: Field<T>) {
        operator fun rangeTo(value: PhysicalValue<*>) {
            field.setContent(value.castAs(field.type), null, null)
        }
    }

    fun modify(groupName: String, modifier: SubcomponentGroupModifier.() -> Unit) {
        val modifiedGroup = component.getSubcomponentGroup(groupName)
        component.modifySubcomponentGroupContent(
            groupName,
            SubcomponentGroupModifier(modifiedGroup.content).apply(modifier).build()
        )
    }

    fun field(fieldName: String): FieldModifier<*> {
        return FieldModifier(component.getField(fieldName))
    }

    fun build() = component
}