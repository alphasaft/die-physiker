package physics.packaging

import mergedWith
import physics.components.ComponentClass


class Module(val name: String, private val classes: Map<String, ComponentClass>) {
    operator fun get(className: String): ComponentClass {
        require(className in classes) { "No such class in module '$name' : '$className'" }

        return classes.getValue(className)
    }

    internal fun unpack(): Map<String, ComponentClass> {
        return classes
    }

    operator fun plus(other: Module): Module {
        fun merge(key: String, c1: ComponentClass, c2: ComponentClass): ComponentClass {
            require(c1 === c2) { "When merging $name and ${other.name} : Crashing declarations for $key." }
            return c1
        }

        return Module(name, classes.mergedWith(other.classes, merge = ::merge))
    }
}
