package physics.components

import physics.quantities.PValue


class ComponentModifier(val component: Component) {

    class GroupModifier(private val group: Group) {
        private val toRemove = mutableListOf<Component>()
        private val toAdd = mutableListOf<Component>()

        operator fun Component.unaryMinus() { toRemove.add(this) }
        operator fun Component.unaryPlus() { toAdd.add(this) }

        internal fun validate() {
            toRemove.forEach(group::removeElement)
            toAdd.forEach(group::addElement)
        }
    }

    class FieldModifier<T : PValue<T>>(private val field: Field<T>) {
        operator fun rangeTo(value: T) {
            field.setContent(value)
        }
    }

    fun group(groupName: String, modifier: GroupModifier.() -> Unit) {
        val modifiedGroup = component.getGroup(groupName)
        GroupModifier(modifiedGroup).apply(modifier).validate()
    }

    fun field(fieldName: String): FieldModifier<*> {
        return FieldModifier(component.getField(fieldName))
    }

    override fun toString(): String {
        return "<Modifier of $component>"
    }
}
