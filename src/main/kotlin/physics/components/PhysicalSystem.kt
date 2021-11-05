package physics.components

import physics.values.PhysicalValue

class PhysicalSystem(val components: List<Component>) {
    constructor(vararg components: Component): this(components.toList())

    fun allComponents(): List<Component> {
        return (components + components.map { it.allSubcomponents() }.flatten())
    }

    fun <T : PhysicalValue<*>> findFieldOwner(field: Field<T>): Component {
        return allComponents().single { c -> c.fields.any { field === it } }
    }
}
