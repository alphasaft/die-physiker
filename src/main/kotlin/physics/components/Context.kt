package physics.components

import physics.quantities.PValue


class Context(components: List<Component>) {
    constructor(vararg components: Component): this(components.toList())

    private val components = components.toMutableList()

    fun allComponents(): List<Component> {
        return (components + components.map { it.allSubcomponents() }.flatten())
    }

    fun findComponentOwner(component: Component): Component? {
        return allComponents().singleOrNull { it.subcomponentsGroups.flatten().any { c -> c === component } }
    }

    fun <T : PValue<T>> findFieldOwner(field: Field<T>): Component {
        return allComponents().single { c -> c.fields.any { field === it } }
    }
}
