package physics

import physics.specs.ComponentSpec
import physics.specs.FieldAccessSpec
import physics.specs.RootComponentSpec
import util.*


class Formula(
    private val rootSpec: RootComponentSpec,
    val componentsSpecs: List<ComponentSpec>,
    private val requiredFields: List<FieldAccessSpec>,
    val outputSpec: FieldAccessSpec,
    private val expression: (FormulaArguments) -> Any,
) {
    init { checkComponentsWereDeclared() }

    private fun checkComponentsWereDeclared() {
        val declaredComponents = listOf(rootSpec.name) + componentsSpecs.map { it.name }
        val concernedFieldsThatWasNotDeclared = requiredFields.map { it.fieldOwner }.find { it !in declaredComponents }
        require(concernedFieldsThatWasNotDeclared == null) { "Component $concernedFieldsThatWasNotDeclared, a field owner, wasn't declared" }
    }

    fun <T> computeAs(
        rootComponent: RootPhysicalComponent,
        searchedField: FieldName,
        componentOwningSearchedField: PhysicalComponent
    ): T? {
        if (!isUsefulFor(rootComponent, searchedField, componentOwningSearchedField)) return null

        val constraints = generateImpliedConstraints(componentOwningSearchedField, requiredFields)
        val namedComponents = mutableMapOf<String, PhysicalComponent>()

        namedComponents[rootSpec.name] = rootComponent.takeIf(constraints.getValue(rootSpec.name)) ?: return null

        for (componentSpec in componentsSpecs) {
            val selectedComponents = selectComponentsFor(
                componentSpec,
                constraint = constraints.getValue(componentSpec.name),
                where = namedComponents
            ) ?: return null

            for ((name, component) in selectedComponents) {
                namedComponents.merge(name, component) { _, _ -> throw IllegalArgumentException("Component $name was declared at least twice.") }
            }
        }

        @Suppress("UNCHECKED_CAST")
        return expression(FormulaArguments(namedComponents)) as T
    }

    private fun isUsefulFor(
        rootComponent: RootPhysicalComponent,
        searchedField: FieldName,
        componentOwningSearchedField: PhysicalComponent
    ): Boolean {
        val rootComponentTypeSuits = rootComponent.typeName == rootSpec.type
        val outputFieldSuits = searchedField == outputSpec.fieldName
        val outputComponentTypeSuits = when {
            componentOwningSearchedField === rootComponent -> outputSpec.fieldOwner == rootSpec.name
            outputSpec.fieldOwner == rootSpec.name -> componentOwningSearchedField === rootComponent
            else -> componentOwningSearchedField.typeName == rootComponent.typeOfSubComponent(findSpecFor(outputSpec.fieldOwner)!!.storedInto)
        }
        return rootComponentTypeSuits && outputFieldSuits && outputComponentTypeSuits
    }


    private fun generateImpliedConstraints(
        componentOwningSearchedField: PhysicalComponent,
        requiredFields: List<FieldAccessSpec>,
    ): Map<String, Predicate<PhysicalComponent>> {

        val constraints = generateSearchedFieldRelatedConstraints(componentOwningSearchedField).toMutableMap()
        constraints.mergeWith(generateRequiredFieldsConstraints(requiredFields), merge = { old, new -> { old(it) && new(it) } })
        return constraints
    }

    private fun generateRequiredFieldsConstraints(
        requiredFields: List<FieldAccessSpec>
    ): Map<String, Predicate<PhysicalComponent>> {

        val constraints = mutableMapOf<String, Predicate<PhysicalComponent>>()
        for ((component, field) in requiredFields.map { it.fieldOwner to it.fieldName }) {
            constraints.merge(component, { it.getField<Any>(field) != null }) { old, new -> { old(it) && new(it) } }
        }
        return constraints
    }

    private fun generateSearchedFieldRelatedConstraints(
        componentOwningSearchedField: PhysicalComponent,
    ): Map<String, Predicate<PhysicalComponent>> {

        val constraints = mutableMapOf<String, Predicate<PhysicalComponent>>(outputSpec.fieldOwner to { it === componentOwningSearchedField })
        var parentSpec = findSpecFor(outputSpec.fieldOwner) ?: return constraints
        while (true) {
            constraints[parentSpec.parentName] = { it.hasSubComponent(componentOwningSearchedField) }
            parentSpec = findSpecFor(parentSpec.parentName) ?: break
        }

        return constraints
    }

    private fun findSpecFor(componentName: String) =
        componentsSpecs.find { it.name == componentName }

    private fun selectComponentsFor(
        spec: ComponentSpec,
        constraint: Predicate<PhysicalComponent>,
        where: Map<String, PhysicalComponent>
    ): Map<String, PhysicalComponent>? {
        requireNotNull(spec.location)
        return when (spec.select) {
            '?' -> selectSingleComponentFor(spec, constraint, where)?.let { mapOf(it) }
            '*' -> selectAllComponentsFor(spec, constraint, where)
            '+' -> selectOnlyYetUnselectedComponentsFor(spec, constraint, where)
            else -> throw IllegalArgumentException("Invalid ComponentSpec flag ${spec.select}")
        }
    }

    private fun selectSingleComponentFor(
        spec: ComponentSpec,
        constraint: Predicate<PhysicalComponent>,
        where: Map<String, PhysicalComponent>
    ): Pair<String, PhysicalComponent>? {
        return where
            .getValue(spec.parentName)
            .getSubComponents(spec.storedInto)
            .find(constraint)
            ?.let { spec.name to it }
    }

    private fun selectAllComponentsFor(
        spec: ComponentSpec,
        constraint: Predicate<PhysicalComponent>,
        where: Map<String, PhysicalComponent>
    ): Map<String, PhysicalComponent>? {
        return where
            .getValue(spec.parentName)
            .getSubComponents(spec.storedInto)
            .takeIf { it.all { component -> constraint(component) } }
            ?.withIndex()
            ?.associateBy { (index, _) -> spec.name + (index + 1) }  // First index is #1
            ?.mapValues { (_, indexedValue) -> indexedValue.value }
    }

    private fun selectOnlyYetUnselectedComponentsFor(
        spec: ComponentSpec,
        constraint: Predicate<PhysicalComponent>,
        where: Map<String, PhysicalComponent>
    ): Map<String, PhysicalComponent>? {
        return where
            .getValue(spec.parentName)
            .getSubComponents(spec.storedInto)
            .filter { component -> !where.any { (_, value) -> component === value } }
            .takeIf { it.all { component -> constraint(component) } }
            ?.withIndex()
            ?.associateBy { (index, _) -> spec.name + (index + 1) }  // First index is #1
            ?.mapValues { (_, indexedValue) -> indexedValue.value }
    }
}
