package physics.components

import physics.values.PhysicalValue
import physics.values.castAs

class ComponentModifier(val component: Component) {

    class SubcomponentGroupModifier(private val group: ComponentGroup) {
        private val toRemove = mutableListOf<Component>()
        private val toAdd = mutableListOf<Component>()

        operator fun Component.unaryMinus() { toRemove.add(this) }
        operator fun Component.unaryPlus() { toAdd.add(this) }

        internal fun validate() {
            toRemove.forEach(group::removeElement)
            toAdd.forEach(group::addElement)
        }
    }

    class FieldModifier<T : PhysicalValue<*>>(private val field: Field<T>) {
        operator fun rangeTo(value: PhysicalValue<*>) {
            field.setContent(value.castAs(field.type), null, null)
        }
    }

    fun group(groupName: String, modifier: SubcomponentGroupModifier.() -> Unit) {
        val modifiedGroup = component.getSubcomponentGroup(groupName)
        SubcomponentGroupModifier(modifiedGroup).apply(modifier).validate()
    }

    fun field(fieldName: String): FieldModifier<*> {
        return FieldModifier(component.getField(fieldName))
    }

    override fun toString(): String {
        return "<Modifier of $component>"
    }
}