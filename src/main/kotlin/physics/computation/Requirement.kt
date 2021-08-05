package physics.computation

import physics.NoComponentMatchingRequirementsFoundException
import physics.UndeclaredComponentException
import physics.components.Component
import physics.components.ComponentClass
import physics.components.Field
import physics.components.PhysicalSystem
import physics.values.PhysicalValue

class Requirement private constructor(
    val alias: String,
    internal val type: ComponentClass,
    private val fullLocation: String?,
    val ownedVariables: Map<String, String>,
    val selectAll: Boolean = false,
) {
    companion object Factory {
        fun single(alias: String, a: ComponentClass, from: String?, withVariables: Map<String, String>) =
            Requirement(alias, a, from, withVariables, selectAll = false)

        fun multi(the: String, are: ComponentClass, from: String?, withVariables: Map<String, String>) =
            Requirement(the, are, from, withVariables, selectAll = true)
    }

    init {
        when (selectAll) {
            true -> {
                require("#" in alias) { "Expected '#' in component alias." }
                require(ownedVariables.keys.all { "#" in it }) { "Expected '#' in each variable name." }
            }
            false -> {
                require("#" !in alias) { "Expected no '#' in component alias." }
                require(ownedVariables.keys.none { "#" in it }) { "Expected no '#' in any of the variables names." }
            }
        }
    }

    private val ownerName = fullLocation?.split(".")?.first()
    private val location = fullLocation?.split(".")?.last()

    infix fun matches(component: Component): Boolean = component instanceOf type && ownedVariables.values.all { component.getOrNull<PhysicalValue<*>>(it) != null }
    infix fun requiresField(field: String) = ownedVariables.values.any { it == field }

    fun selectAppropriateComponentsIn(
        system: PhysicalSystem,
        alreadySelected: Map<String, Component>,
    ): Map<String, Component> =
        if (selectAll) selectAllIn(system, alreadySelected)
        else when {
            ownerName == null -> if (alias !in alreadySelected) mapOf(selectAtRootIn(system)) else emptyMap()
            alias in alreadySelected -> mapOf(selectParentIn(system, alreadySelected))
            ownerName in alreadySelected -> mapOf(selectSingleIn(ownerName, alias, alreadySelected))
            else -> throw NoWhenBranchMatchedException()
        }

    private fun selectAtRootIn(system: PhysicalSystem): Pair<String, Component> =
        alias to (system.components.find { this matches it } ?: throw NoComponentMatchingRequirementsFoundException("<root>", "<all>", ownedVariables.values))

    private fun indexed(s: String, i: Int) = s.replace("#", i.toString())

    private fun selectAllIn(system:PhysicalSystem, alreadySelected: Map<String, Component>): Map<String, Component> {
        if (ownerName == null) return system.components
            .filter { this matches it }
            .mapIndexed { i, it -> indexed(alias, i) to it }
            .toMap()

        var i = 1
        val result = mutableMapOf<String, Component>()
        while (true) {
            val found =
                try { selectSingleIn(indexed(ownerName, i), indexed(ownerName, i), alreadySelected) }
                catch (e: UndeclaredComponentException) { break }
            result[found.first] = found.second
            i++
        }
        return result
    }

    private fun selectSingleIn(
        owner: String,
        storeAs: String,
        alreadySelected: Map<String, Component>,
    ): Pair<String, Component> {
        requireNotNull(ownerName)
        requireNotNull(location)

        val selectedOwner = alreadySelected[owner] ?: throw UndeclaredComponentException(ownerName)
        return storeAs to (selectedOwner
            .getSubcomponentGroup(location)
            .find { !selectAll && this matches it }
            ?: throw NoComponentMatchingRequirementsFoundException(owner, location, ownedVariables.values))
    }

    private fun selectParentIn(system: PhysicalSystem, alreadySelected: Map<String, Component>): Pair<String, Component> {
        requireNotNull(ownerName)
        requireNotNull(location)

        val that = alreadySelected.getValue(alias)
        return ownerName to system
            .allComponents()
            .filter { it.hasSubcomponentGroup(location) }
            .first { that in it.getSubcomponentGroup(location) }
    }

    fun fetchVariablesIn(registeredComponents: Map<String, Component>): Map<String, PhysicalValue<*>> {
        return fetchFieldsIn(registeredComponents).mapValues { (_, field) -> field.getContent() }
    }

    fun fetchFieldsIn(registeredComponents: Map<String, Component>): Map<String, Field<*>> {
        return if (selectAll) fetchAllMatchingFieldIn(registeredComponents)
        else fetchSingleMatchingFieldIn(registeredComponents)
    }

    private fun fetchAllMatchingFieldIn(registeredComponents: Map<String, Component>): Map<String, Field<*>> {
        val result = mutableMapOf<String, Field<*>>()
        var i = 0
        while (true) {
            val component = registeredComponents[indexed(alias, i)] ?: break
            for ((variable, backingField) in ownedVariables) result[indexed(variable, i)] = component.getField(backingField)
            i++
        }
        return result
    }

    private fun fetchSingleMatchingFieldIn(registeredComponents: Map<String, Component>): Map<String, Field<*>> {
        val component = registeredComponents.getValue(alias)
        val result = mutableMapOf<String, Field<*>>()
        for ((variable, backingField) in ownedVariables) result[variable] = component.getField(backingField)
        return result
    }

    fun fuseWith(other: Requirement): Requirement {
        require(type inheritsOf other.type) { "Can't fuse requirement of type $type and ${other.type}" }
        require(location == other.location) { "Can't fuse requirements that ain't located themselves in the same place." }
        require(selectAll && !other.selectAll) { "Can't fuse multi-requirements." }

        return Requirement(
            alias,
            type,
            fullLocation,
            ownedVariables + other.ownedVariables,
            selectAll = false
        )
    }

    fun withOptionalField(field: String) = withOptionalFields(listOf(field))
    private fun withOptionalFields(fields: List<String>) = Requirement(
        alias,
        type,
        fullLocation,
        ownedVariables.filterValues { it !in fields },
        selectAll
    )

    fun withRequiredVariable(variableName: String, backingField: String) = withRequiredVariables(mapOf(variableName to backingField))
    private fun withRequiredVariables(variables: Map<String, String>) = Requirement(
        alias,
        type,
        fullLocation,
        ownedVariables + variables,
        selectAll
    )

    fun withAlias(alias: String) = Requirement(
        alias,
        type,
        fullLocation,
        ownedVariables,
        selectAll
    )
}
