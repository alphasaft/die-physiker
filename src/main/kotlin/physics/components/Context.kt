package physics.components

import physics.quantities.PValue


class Context(components: List<Component>) {
    private val components = components.toMutableList()

    fun allComponents(): List<Component> {
        return (components + components.map { it.allSubcomponents() }.flatten())
    }

    fun findComponentOwner(component: Component): Component? {
        return allComponents().singleOrNull { it.boxes.values.flatten().any { c -> c === component } }
    }

    fun <T : PValue<T>> findFieldOwner(field: Field<T>): Component {
        return allComponents().single { c -> c.fields.any { field === it } }
    }
}
