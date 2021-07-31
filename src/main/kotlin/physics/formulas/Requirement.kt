package physics.formulas

import physics.NoComponentMatchingRequirementsFoundException
import physics.UndeclaredComponentException
import physics.components.Component
import physics.components.ComponentClass
import physics.components.PhysicalSystem
import physics.values.PhysicalValue


class Requirement(
    location: String?,
    private val type: ComponentClass,
    private val requiredFields: List<String>,
    val name: String,
    private val selectAll: Boolean = false,
) {
    private val ownerName = location?.split(".")?.first()
    private val locationFieldName = location?.split(".")?.last()

    override fun toString(): String {
        return "$type, $requiredFields, $name, $selectAll"
    }

    fun selectIn(
        system: PhysicalSystem,
        alreadySelected: Map<String, Component>,
    ): Map<String, Component> =
        if (selectAll) selectAllIn(alreadySelected)
        else when {
            name !in alreadySelected && ownerName == null -> mapOf(selectAtRootIn(system))
            name in alreadySelected && ownerName == null -> emptyMap()
            name in alreadySelected -> mapOf(selectParentIn(system, alreadySelected))
            ownerName in alreadySelected -> mapOf(selectSingleIn(ownerName!!, name, alreadySelected))
            else -> throw NoWhenBranchMatchedException()
        }

    private fun selectAtRootIn(system: PhysicalSystem): Pair<String, Component> {
        return name to (system.components
            .find { it instanceOf type && requiredFields.all { f -> it.getOrNull<PhysicalValue<*>>(f) != null } }
            ?: throw NoComponentMatchingRequirementsFoundException("<root>", "<all>", requiredFields))
    }

    private fun String.indexed(i: Int) = this.replace("#", i.toString())

    private fun selectAllIn(alreadySelected: Map<String, Component>): Map<String, Component> {
        requireNotNull(ownerName)

        var i = 1
        val result = mutableMapOf<String, Component>()
        while (true) {
            val found =
                try { selectSingleIn(ownerName.indexed(i), name.indexed(i), alreadySelected) }
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
        requireNotNull(locationFieldName)

        val selectedOwner = alreadySelected[owner] ?: throw UndeclaredComponentException(ownerName)
        return storeAs to (selectedOwner
            .getSubcomponentGroup(locationFieldName)
            .find { c -> c instanceOf type && requiredFields.all { f -> c.getOrNull<PhysicalValue<*>>(f) != null } }
            ?: throw NoComponentMatchingRequirementsFoundException(owner, locationFieldName, requiredFields))
    }

    private fun selectParentIn(system: PhysicalSystem, alreadySelected: Map<String, Component>): Pair<String, Component> {
        requireNotNull(ownerName)
        requireNotNull(locationFieldName)

        val that = alreadySelected.getValue(name)
        return ownerName to system
            .fetchRecursivelyAllComponents()
            .filter { it.hasSubcomponentGroup(locationFieldName) }
            .first { that in it.getSubcomponentGroup(locationFieldName) }
    }

    fun withOptionalField(field: String) = withOptionalFields(listOf(field))
    private fun withOptionalFields(fields: List<String>) = Requirement(
        ownerName?.let { "$ownerName.$locationFieldName" },
        type,
        requiredFields - fields,
        name,
        selectAll
    )

    fun withRequiredField(field: String) = withRequiredFields(listOf(field))
    private fun withRequiredFields(fields: List<String>) = Requirement(
        ownerName?.let { "$ownerName.$locationFieldName" },
        type,
        requiredFields + fields,
        name,
        selectAll
    )

    fun withName(name: String) = Requirement(
        ownerName?.let { "$ownerName.$locationFieldName" },
        type,
        requiredFields,
        name,
        selectAll
    )

    private infix fun matches(component: Component): Boolean = component instanceOf type && requiredFields.all { component.getOrNull<PhysicalValue<*>>(it) != null }
    infix fun matchesSingle(component: Component): Boolean = !selectAll && this matches component

    fun requiresField(field: String) = field in requiredFields
}
