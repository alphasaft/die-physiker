package physics.components2

import Args
import alwaysTrue
import physics.components.Component
import physics.components.ComponentClass
import physics.components.Location

class ComponentSpec(
    val type: ComponentClass,
    val location: Location,
    val predicate: (that: Component, picks: Args<Component>) -> Boolean = ::alwaysTrue,
) {
    infix fun matches(component: Component): Boolean {
        return component instanceOf type
    }

    fun validates(picked: Component, otherPicks: Map<String, Component>): Boolean {
        return locationMatches(picked, otherPicks) && predicate(picked, otherPicks)
    }

    private fun locationMatches(picked: Component, otherPicks: Args<Component>): Boolean {
        return if (location is Location.At) otherPicks.getValue(location.alias).getGroup(location.field).any { it === picked }
        else true
    }
}