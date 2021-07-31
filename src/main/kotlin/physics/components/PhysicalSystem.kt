package physics.components

import physics.values.PhysicalValue

class PhysicalSystem(val components: List<Component>) {
    constructor(vararg components: Component): this(components.toList())

    fun fetchRecursivelyAllComponents(): Set<Component> {
        return (components + components.map { it.allSubcomponents() }.flatten()).toSet()
    }

    fun <T : PhysicalValue<*>> fetchFieldOwner(field: Field<T>): Component {
        return fetchRecursivelyAllComponents().single { c -> c.fields.any { field === it } }
    }
}
