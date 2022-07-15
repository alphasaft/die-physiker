package physics.components

import physics.quantities.PValue


class ComponentModifier(val component: Component) {

    class BoxModifier internal constructor(private val box: ComponentBox) {
        private val toRemove = mutableListOf<Component>()
        private val toAdd = mutableListOf<Component>()

        operator fun Component.unaryMinus() { toRemove.add(this) }
        operator fun Component.unaryPlus() { toAdd.add(this) }

        internal fun validate() {
            toRemove.forEach(box::removeElement)
            toAdd.forEach(box::addElement)
        }
    }

    class FieldModifier<T : PValue<T>> internal constructor(private val field: Field<T>) {
        operator fun rangeTo(value: T) {
            field.setContent(value)
        }
    }

    fun group(groupName: String, modifier: BoxModifier.() -> Unit) {
        val modifiedGroup = component.getBox(groupName)
        BoxModifier(modifiedGroup).apply(modifier).validate()
    }

    fun field(fieldName: String): FieldModifier<*> {
        return FieldModifier(component.getField(fieldName))
    }

    override fun toString(): String {
        return "<Modifier of $component>"
    }
}
