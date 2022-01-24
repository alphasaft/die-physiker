package physics.components2

import isIncludedIn
import physics.components.Component
import physics.components.Context


class ComponentsPicker(vararg specs: Pair<String, ComponentSpec>) {
    private val specs = specs.toMap()
    private val aliases = this.specs.keys.toList()

    init {
        for (alias in this.specs.keys.filter { it.any { c -> c.isDigit() } }) {
            val correspondingGenericAlias = alias.replace(Regex("[0-9]+"), "#")
            require(correspondingGenericAlias !in this.specs) { "Couldn't expand properly the plural specs because $correspondingGenericAlias would crash with $alias." }
        }
    }

    private fun String.comesFromGenericAlias() = any { it.isDigit() } && replace(Regex("[0-9]+"), "#") in specs

    private fun expandPluralSpecs(components: List<Component>): Map<String, ComponentSpec> {
        val expanded = mutableMapOf<String, ComponentSpec>()
        for ((alias, spec) in specs) {
            if ('#' !in alias) expanded[alias] = spec
            else for (i in 1..components.count { spec matches it }) expanded[alias.replace("#", i.toString())] = spec
        }
        return expanded
    }

    fun pickAllComponents(
        context: Context,
        initialComponents: Map<String, Component> = emptyMap(),
    ): List<Map<String, Component>> {
        val components = context.allComponents()
        val specs = expandPluralSpecs(components = components)
        val acceptedAliasesByComponent = components.associateWith { c -> specs.filter { (_, s) -> s matches c }.map { it.key } }

        fun List<Component>.generatePossiblePicks(): List<Map<String, Component>> {
            if (isEmpty()) return listOf(emptyMap())

            val head = first()
            val tail = drop(1)

            return tail
                .generatePossiblePicks()
                .map { picks -> (aliases - picks.keys).map { id -> picks + Pair(id, head) } + picks }
                .flatten()
                .filter { picks -> picks.all { (alias, c) -> (alias in acceptedAliasesByComponent.getValue(c)) } }
        }

        val possiblePicks = components
            .generatePossiblePicks()
            .filter { initialComponents isIncludedIn it }
            .filter { arePluralsOrdered(it) }
            .filter { specs.all { (alias, spec) -> (alias.comesFromGenericAlias() || alias in it) && spec.validates(it.getValue(alias), it) } }

        val standardSize = possiblePicks.maxOf { it.size }
        return possiblePicks.filter { it.size == standardSize }
    }

    private fun arePluralsOrdered(picks: Map<String, Component>): Boolean {
        for (genericAlias in specs.keys.filter { "#" in it }) {
            var i = 1
            while (true) {
                val alias = genericAlias.replace("#", i.toString())
                if ((alias !in picks)) {
                    if (picks.keys.count { it.replace(Regex("[0-9]+"), "#") == genericAlias } != i - 1) return false
                    break
                }
                i++
            }
        }
        return true
    }
}
