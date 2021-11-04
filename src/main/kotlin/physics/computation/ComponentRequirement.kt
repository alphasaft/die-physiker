package physics.computation

import physics.EndOfMultiSelection
import physics.NoComponentMatchingRequirementsFoundException
import physics.UndeclaredComponentException
import physics.components.Component
import physics.components.ComponentClass
import physics.components.Field
import physics.components.PhysicalSystem
import physics.values.PhysicalValue
import println

class ComponentRequirement private constructor(
    val alias: String,
    internal val type: ComponentClass,
    private val fullLocation: String?,
    val ownedVariables: Map<String, String>,
    val selectAll: Boolean = false,
    val forbiddenOverlappingAliases: Set<String>,
) {
    companion object Factory {
        fun single(alias: String, type: ComponentClass, location: String?, variables: Map<String, String>) =
            ComponentRequirement(alias, type, location, variables, selectAll = false, emptySet())

        fun allRemaining(alias: String, type: ComponentClass, location: String?, variables: Map<String, String>) =
            ComponentRequirement(alias, type, location, variables, selectAll = true, emptySet())
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

    val isLocatedAtRoot get() = fullLocation == null
    internal val ownerAlias = fullLocation?.split(".")?.first()
    val location = fullLocation?.split(".")?.last()

    infix fun matches(component: Component): Boolean = component instanceOf type && ownedVariables.values.all { component.getOrNull<PhysicalValue<*>>(it) != null }
    infix fun requiresField(field: String) = ownedVariables.values.any { it == field }

    fun selectAppropriateComponentsIn(
        system: PhysicalSystem,
        alreadySelected: Map<String, Component>,
    ): Map<String, Component> {
        return if (selectAll) selectAllIn(system, alreadySelected)
        else when {
            ownerAlias == null -> if (alias !in alreadySelected) mapOf(selectAtRootIn(system)) else emptyMap()
            alias in alreadySelected -> mapOf(selectParentIn(system, alreadySelected))
            ownerAlias in alreadySelected -> mapOf(selectSingleIn(ownerAlias, alias, alreadySelected))
            else -> throw NoWhenBranchMatchedException()
        }
    }

    private fun selectAtRootIn(system: PhysicalSystem): Pair<String, Component> =
        alias to (system.components.find { this matches it } ?: throw NoComponentMatchingRequirementsFoundException("<root>", "<all>", ownedVariables.values))

    private fun indexed(s: String, i: Int) = s.replace("#", i.toString())

    private fun selectAllIn(system:PhysicalSystem, alreadySelected: Map<String, Component>): Map<String, Component> {
        if (ownerAlias == null) return system.components
            .filter { this matches it }
            .mapIndexed { i, it -> indexed(alias, i) to it }
            .toMap()

        val alreadySelectedAsMutableMap = alreadySelected.toMutableMap()

        var i = 1
        val result = mutableMapOf<String, Component>()
        while (true) {
            val found =
                try { selectSingleIn(ownerAlias, indexed(alias, i), alreadySelectedAsMutableMap) }
                catch (e: EndOfMultiSelection) { break }

            result[found.first] = found.second
            alreadySelectedAsMutableMap[found.first] = found.second
            i++
        }

        return result
    }

    private fun selectSingleIn(
        owner: String,
        storeAs: String,
        alreadySelected: Map<String, Component>,
    ): Pair<String, Component> {
        requireNotNull(ownerAlias)
        requireNotNull(location)

        val selectedOwner = alreadySelected[owner] ?: throw UndeclaredComponentException(ownerAlias)
        val subcomponentsGroup = selectedOwner.getSubcomponentGroup(location)
        val selected = subcomponentsGroup.filter { this matches it }

        fun isAlreadySelected(component: Component): Boolean =
            alreadySelected.any { (alias, otherComponent) ->
                (alias in forbiddenOverlappingAliases || (alias.replace(Regex("(?!-)[0-9]+"), "#")) in forbiddenOverlappingAliases)
                && component === otherComponent
            }

        if (selected.all(::isAlreadySelected) && selectAll) throw EndOfMultiSelection()
        else return storeAs to (selected
                    .firstOrNull { !isAlreadySelected(it) }
                    ?: throw NoComponentMatchingRequirementsFoundException(owner, location, ownedVariables.values))
    }

    private fun selectParentIn(system: PhysicalSystem, alreadySelected: Map<String, Component>): Pair<String, Component> {
        requireNotNull(ownerAlias)
        requireNotNull(location)

        val that = alreadySelected.getValue(alias)
        return ownerAlias to system
            .allComponents()
            .filter { it.hasSubcomponentGroup(location) }
            .first { that in it.getSubcomponentGroup(location) }
    }

    fun fetchVariablesValuesIn(selectedComponents: Map<String, Component>): Map<String, PhysicalValue<*>> {
        return fetchRequiredFieldsIn(selectedComponents).mapValues { (_, field) -> field.getContent() }
    }

    fun fetchRequiredFieldsIn(selectedComponents: Map<String, Component>): Map<String, Field<*>> {
        return if (selectAll) fetchAllMatchingFieldsIn(selectedComponents)
        else fetchSingleMatchingFieldIn(selectedComponents)
    }

    private fun fetchAllMatchingFieldsIn(registeredComponents: Map<String, Component>): Map<String, Field<*>> {
        val result = mutableMapOf<String, Field<*>>()
        var i = 1
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

    private fun copy(
        alias: String = this.alias,
        type: ComponentClass = this.type,
        ownerAlias: String? = this.ownerAlias,
        location: String? = this.location,
        ownedVariables: Map<String, String> = this.ownedVariables,
        selectAll: Boolean = this.selectAll,
        forbiddenOverlappingAliases: Set<String> = this.forbiddenOverlappingAliases,
    ) =
        ComponentRequirement(
            alias,
            type,
            if (ownerAlias == null) null else "$ownerAlias.$location",
            ownedVariables,
            selectAll,
            forbiddenOverlappingAliases
        )

    fun fuseWith(other: ComponentRequirement): ComponentRequirement {
        require(type inheritsOf other.type) { "Can't fuse requirement of type $type and ${other.type}" }
        require(location == other.location) { "Can't fuse requirements that aren't located in the same place." }
        require(!selectAll && !other.selectAll) { "Can't fuse multi-requirements." }

        // TODO : Avoid name crashes

        return copy(
            ownedVariables = ownedVariables + other.ownedVariables,
            forbiddenOverlappingAliases = forbiddenOverlappingAliases + other.forbiddenOverlappingAliases
        )
    }

    fun withAliasReferenceUpdated(old: String, new: String) = withAliasesReferencesUpdated(mapOf(old to new))
    fun withAliasesReferencesUpdated(oldAliasesLinkedToNewOnes: Map<String, String>): ComponentRequirement {
        val newAlias = oldAliasesLinkedToNewOnes[alias] ?: alias
        val newOwnerAlias = oldAliasesLinkedToNewOnes[ownerAlias] ?: ownerAlias
        val newForbiddenOverlappingAliases = forbiddenOverlappingAliases.map { oldAliasesLinkedToNewOnes[it] ?: it }.toSet()
        return copy(alias = newAlias, ownerAlias = newOwnerAlias, forbiddenOverlappingAliases = newForbiddenOverlappingAliases)
    }

    fun withOptionalVariable(variable: String) = withOptionalVariables(listOf(variable))
    private fun withOptionalVariables(variables: List<String>) = copy(ownedVariables = ownedVariables.filterKeys { it !in variables })

    fun withOptionalField(field: String) = withOptionalFields(listOf(field))
    private fun withOptionalFields(fields: List<String>) = copy(ownedVariables = ownedVariables.filterValues { it !in fields })

    fun withRequiredVariable(variableName: String, backingField: String) = withRequiredVariables(mapOf(variableName to backingField))
    private fun withRequiredVariables(variables: Map<String, String>) = copy(ownedVariables = ownedVariables + variables)

    fun withAlias(alias: String) = copy(alias = alias)

    internal fun withFollowingOverlappingAliasesForbidden(aliases: Set<String>) = copy(forbiddenOverlappingAliases = forbiddenOverlappingAliases + aliases)

    override fun toString(): String {
        return alias
    }
}
