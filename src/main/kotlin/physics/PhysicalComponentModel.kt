package physics

import physics.specs.FieldSpec
import util.println
import util.toMutableMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


open class PhysicalComponentModel(
    protected val name: String,
    fieldsSpecs: List<FieldSpec>,
    protected val subComponentsNames: Map<String, ComponentTypeName> = emptyMap()
) {
    private val fieldsSpecs = fieldsSpecs.associate { it.name to it.type }

    companion object FormulaRegister {
        val formulas = mutableListOf<Formula>()

        fun addFormula(formula: Formula) { formulas.add(formula) }
        fun addFormulas(formulas: List<Formula>) { FormulaRegister.formulas.addAll(formulas) }
    }

    /** 'Constructor' for PhysicalComponent */
    open operator fun invoke(
        vararg fields: Pair<String, Any?>,
        subComponents: Map<String, List<Instance>> = emptyMap()
    ): Instance {
        return Instance(fields.toMap(), subComponents)
    }

    open inner class Instance internal constructor(
        fields: Map<String, Any?>,
        subComponents: Map<String, List<Instance>>
    ) {
        private val registeredFields = fieldsSpecs.mapValues { UNKNOWN }.toMutableMap<String, Any?>()
        private val registeredSubComponents = subComponentsNames.keys.associateWith { mutableListOf<PhysicalComponent>() }.toMutableMap()
        val knownFields get() = registeredFields.keys.zip(registeredFields.values.filterNotNull()).toMutableMap()
        val typeName = this@PhysicalComponentModel.name

        private val knownFieldsCount get(): Int =
            registeredFields.values.count { it != UNKNOWN } +
            registeredSubComponents.values.flatten().sumBy { it.knownFieldsCount }

        init {
            for ((name, value) in fields) setField(name, value)
            for ((name, components) in subComponents) registerSubComponents(name, components)
        }

        private fun setField(key: String, value: Any?) {
            require(key in fieldsSpecs) { "Invalid field $key for component $typeName" }
            require(value == UNKNOWN || fieldsSpecs.getValue(key).isInstance(value)) { "Expected type ${fieldsSpecs.getValue(key).simpleName} for field $key, got $value"}
            registeredFields[key] = value
        }

        fun registerSubComponent(key: String, value: Instance) {
            require(key in subComponentsNames) { "Invalid subcomponent name $key for component $typeName "}
            require(value.typeName == subComponentsNames[key]) { "Expected type ${subComponentsNames[key]} for key $key, got ${value.typeName}" }
            registeredSubComponents.getValue(key).add(value)
        }

        fun registerSubComponents(key: String, values: List<Instance>) {
            require(key in subComponentsNames) { "Invalid subcomponent name $key for component $typeName "}
            require(values.all { it.typeName == subComponentsNames[key] }) { "Expected type ${subComponentsNames[key]} for key $key, got ${values.find { it.typeName != subComponentsNames[key] }!!.typeName}" }
            registeredSubComponents.getValue(key).addAll(values)
        }

        inline fun <reified T : Any> getField(fieldName: String): T? = getField(T::class, fieldName)
        fun <T : Any> getField(kClass: KClass<T>, fieldName: String): T? {
            require(fieldName in fieldsSpecs) { "Attempting to access non-existing field $fieldName of component $typeName"}
            require(fieldsSpecs.getValue(fieldName).isSubclassOf(kClass)) { "Cast from ${fieldsSpecs.getValue(fieldName).simpleName} to ${kClass.simpleName} for field $fieldName can't succeed"}
            @Suppress("UNCHECKED_CAST")
            return registeredFields.getValue(fieldName) as T?
        }

        fun getSubComponent(subComponentName: String, predicate: (Instance) -> Boolean = { true }): Instance? {
            return getSubComponents(subComponentName).find(predicate)
        }

        fun getSubComponents(subComponentsFieldName: String): List<Instance> {
            require(subComponentsFieldName in subComponentsNames) { "Attempting to access non-existing subcomponent field $subComponentsFieldName of component $typeName"}
            return registeredSubComponents.getValue(subComponentsFieldName)
        }

        fun hasSubComponent(subComponent: Instance): Boolean {
            val allSubComponents = registeredSubComponents.values.flatten()
            return allSubComponents.any { it === subComponent } || allSubComponents.any { it.hasSubComponent(subComponent) }
        }

        fun computeAll(root: RootPhysicalComponent) {
            do {
                val oldKnownFieldsCount = knownFieldsCount
                computeUnknownFields(root)
                computeUnknownFieldsOfSubcomponents(root)
            } while (oldKnownFieldsCount != knownFieldsCount)
        }

        private fun computeUnknownFields(root: RootPhysicalComponent) {
            for ((name, field) in registeredFields) {
                registeredFields[name] = field ?: computeUnknownField(name, root)
            }
        }

        private fun computeUnknownField(fieldName: String, root: RootPhysicalComponent): Any? {
            println(fieldName)
            println(formulas.associate { it.componentsSpecs to it.outputSpec to it.computeAs<Any>(root, fieldName, this) }.filterValues { it != null })
            return formulas.mapNotNull { it.computeAs<Any>(root, fieldName, this) }.also(::println).firstOrNull()
        }

        private fun computeUnknownFieldsOfSubcomponents(root: RootPhysicalComponent) {
            registeredSubComponents.values.forEach { subComponentList ->
                subComponentList.forEach { subComponent ->
                    subComponent.computeAll(root)
                    subComponent.computeUnknownFieldsOfSubcomponents(root)
                }
            }
        }

        fun typeOfSubComponent(subComponentsField: String) = subComponentsNames[subComponentsField]!!
    }

}
