package physics.components

import alwaysTrue
import physics.*
import physics.quantities.PValue
import physics.quantities.Quantity
import java.lang.Exception


class ComponentSpec private constructor(
    val alias: String,
    internal val type: ComponentClass,
    internal val location: Location,
    val ownedVariables: Map<String, String>,
    private val condition: (Component, Map<String, Component>) -> Boolean,
    val selectAll: Boolean = false,
    private val forbiddenOverlappingAliases: Set<String>,
) {
    companion object Factory {
        fun single(
            alias: String,
            type: ComponentClass,
            location: Location,
            variables: Map<String, String>,
            condition: (Component, Map<String, Component>) -> Boolean = ::alwaysTrue
        ) = ComponentSpec(alias, type, location, variables, condition, selectAll = false, emptySet())

        fun allRemaining(
            alias: String,
            type: ComponentClass,
            location: Location,
            variables: Map<String, String>,
            condition: (Component, Map<String, Component>) -> Boolean = ::alwaysTrue
        ) = ComponentSpec(alias, type, location, variables, condition, selectAll = true, emptySet())
    }

    init {
        if (selectAll) {
            require("#" in alias) { "Expected '#' in component alias." }
            require(ownedVariables.keys.all { "#" in it }) { "Expected '#' in each variable name." }
        } else {
            require("#" !in alias) { "Expected no '#' in component alias." }
            require(ownedVariables.keys.none { "#" in it }) { "Expected no '#' in any of the variables names." }
        }
    }

    val canBeLocatedAnywhere get() = location is Location.Any
    private val preciseLocation get() = location as Location.At

    private fun matches(component: Component, alreadySelected: Map<String, Component>): Boolean =
        component instanceOf type && condition(component, alreadySelected)

    fun ownsVariable(variable: String): Boolean {
        return variable in ownedVariables
    }

    fun fetchAppropriateComponentsIn(
        context: Context,
        alreadySelected: Map<String, Component>,
    ): Map<String, Component> {
        if (alias in alreadySelected && !this.matches(alreadySelected.getValue(alias), alreadySelected)) {
            val ownerAlias = if (location is Location.At) location.alias else "<root>"
            val fieldLocation = if (location is Location.At) location.field else "<all>"
            throw ComponentsPickerException(
                "No component was found in $ownerAlias.$fieldLocation that provides known values for fields : ${
                    ownedVariables.values.joinToString(
                        ", "
                    )
                }"
            )
        }

        return if (selectAll) selectAllIn(context, alreadySelected)
        else when {
            canBeLocatedAnywhere -> if (alias !in alreadySelected) mapOf(selectAtRootIn(context, alreadySelected)) else emptyMap()
            alias in alreadySelected -> mapOf(selectParentIn(context, alreadySelected))
            preciseLocation.alias in alreadySelected -> mapOf(selectSingleIn(preciseLocation.alias, storeAs = alias, alreadySelected))
            else -> throw NoWhenBranchMatchedException()
        }
    }

    private fun selectAtRootIn(context: Context, alreadySelected: Map<String, Component>): Pair<String, Component> =
        alias to (context.allComponents().find { this.matches(it, alreadySelected) } ?: throw ComponentsPickerException(
            "No component was found in ${"<root>"}.${"<all>"} that provides known values for fields : ${
                ownedVariables.values.joinToString(
                    ", "
                )
            }"
        ))

    object EndOfMultiSelection : Exception()
    private fun indexed(s: String, i: Int) = s.replace("#", i.toString())
    private fun selectAllIn(system: Context, alreadySelected: Map<String, Component>): Map<String, Component> {
        if (canBeLocatedAnywhere) return system.allComponents()
            .filter { this.matches(it, alreadySelected) }
            .mapIndexed { i, it -> indexed(alias, i) to it }
            .toMap()

        val alreadySelectedAsMutableMap = alreadySelected.toMutableMap()

        var i = 1
        val result = mutableMapOf<String, Component>()
        while (true) {
            val found =
                try {
                    selectSingleIn(preciseLocation.alias, indexed(alias, i), alreadySelectedAsMutableMap) }
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
        val selectedOwner = alreadySelected[owner] ?: throw ComponentsPickerException("No component was declared under name '${preciseLocation.alias}'.")
        val subcomponentsGroup = selectedOwner.getGroup(preciseLocation.field)
        val selected = subcomponentsGroup.filter { this.matches(it, alreadySelected) }

        fun isAlreadySelected(component: Component): Boolean =
            alreadySelected.any { (alias, otherComponent) ->
                (alias in forbiddenOverlappingAliases || (alias.replace(Regex("(?!-)[0-9]+"), "#")) in forbiddenOverlappingAliases)
                && component === otherComponent
            }

        if (selected.all(::isAlreadySelected) && selectAll) throw EndOfMultiSelection
        else return storeAs to (selected
            .firstOrNull { !isAlreadySelected(it) }
            ?: throw ComponentsPickerException(
                "No component was found in $owner.${preciseLocation.field} that provides known values for fields : ${
                    ownedVariables.values.joinToString(
                        ", "
                    )
                }"
            ))
    }

    private fun selectParentIn(context: Context, alreadySelected: Map<String, Component>): Pair<String, Component> {
        return preciseLocation.alias to (context.findComponentOwner(alreadySelected.getValue(alias)) ?: throw IllegalArgumentException("System doesn't contain a component that it's supposed to own."))
    }

    fun fetchVariablesValuesIn(selectedComponents: Map<String, Component>): Map<String, Quantity<*>> {
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
        locationAlias: String? = if (canBeLocatedAnywhere) null else this.preciseLocation.alias,
        locationField: String? = if (canBeLocatedAnywhere) null else this.preciseLocation.field,
        ownedVariables: Map<String, String> = this.ownedVariables,
        condition: (Component, Map<String, Component>) -> Boolean = this.condition,
        selectAll: Boolean = this.selectAll,
        forbiddenOverlappingAliases: Set<String> = this.forbiddenOverlappingAliases,
    ) =
        ComponentSpec(
            alias,
            type,
            if (locationAlias == null || locationField == null) Location.Any else Location.At(locationAlias, locationField),
            ownedVariables,
            condition,
            selectAll,
            forbiddenOverlappingAliases
        )

    fun fuseWith(other: ComponentSpec): ComponentSpec {
        require(preciseLocation == other.preciseLocation) { "Can't fuse specs that aren't located in the same place." }
        require(!selectAll && !other.selectAll) { "Can't fuse multi-specs." }

        return copy(
            ownedVariables = ownedVariables + other.ownedVariables,
            forbiddenOverlappingAliases = forbiddenOverlappingAliases + other.forbiddenOverlappingAliases
        )
    }

    fun withAliasReferenceUpdated(old: String, new: String) = withAliasesReferencesUpdated(mapOf(old to new))
    fun withAliasesReferencesUpdated(oldAliasesLinkedToNewOnes: Map<String, String>): ComponentSpec {
        val newAlias = oldAliasesLinkedToNewOnes[alias] ?: alias
        val newOwnerAlias = oldAliasesLinkedToNewOnes[preciseLocation.alias] ?: preciseLocation.alias
        val newForbiddenOverlappingAliases =
            forbiddenOverlappingAliases.map { oldAliasesLinkedToNewOnes[it] ?: it }.toSet()
        return copy(
            alias = newAlias,
            locationAlias = newOwnerAlias,
            forbiddenOverlappingAliases = newForbiddenOverlappingAliases
        )
    }

    fun withOptionalVariable(variable: String) = withOptionalVariables(listOf(variable))
    private fun withOptionalVariables(variables: List<String>) = copy(ownedVariables = ownedVariables.filterKeys { it !in variables })

    fun withOptionalField(field: String) = withOptionalFields(listOf(field))
    private fun withOptionalFields(fields: List<String>) = copy(ownedVariables = ownedVariables.filterValues { it !in fields })

    fun withRequiredVariable(variableName: String, backingField: String) = withRequiredVariables(mapOf(variableName to backingField))
    private fun withRequiredVariables(variables: Map<String, String>) = copy(ownedVariables = ownedVariables + variables)

    internal fun withOverlapsForbidden(aliases: Set<String>) = copy(forbiddenOverlappingAliases = forbiddenOverlappingAliases + aliases)

    override fun toString(): String {
        return alias
    }
}

