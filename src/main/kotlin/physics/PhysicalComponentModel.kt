package physics

import physics.specs.ComponentSpec
import physics.specs.FieldSpec
import physics.specs.ProxySpec
import println
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.typeOf


open class PhysicalComponentModel(
    protected val name: String,
    fieldsSpecs: List<FieldSpec>,
    proxiesSpecs: List<ProxySpec> = emptyList(),
    private val subcomponents: List<ComponentSpec> = emptyList(),
) {
    private val fields = fieldsSpecs.associate { it.name to it.type }
    private val proxies = proxiesSpecs.associate { it.name to it.target }
    private val subcomponentsNames = subcomponents.map { it.name }

    init {
        checkProxies()
    }

    private fun checkProxies() {
        for ((proxyName, proxyTarget) in proxies) {
            val targetedSubcomponent = subcomponents.find { it.name == proxyTarget }
            require(targetedSubcomponent != null) { "Proxy $proxyName targets nothing." }
            require(targetedSubcomponent.atLeast == 1 && targetedSubcomponent.atMost == 1) { "Proxy $proxyName targets several subcomponents at once." }
            require(proxyName !in fields) { "Proxy $proxyName overrides an existing field." }
        }
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

        private val registeredFields = this@PhysicalComponentModel.fields.mapValues { UNKNOWN }.toMutableMap<String, Any?>()
        private val registeredSubcomponents = this@PhysicalComponentModel.subcomponents.associate { it.name to mutableListOf<PhysicalComponent>() }.toMutableMap()
        val knownFields get() = registeredFields.filterValues { it != null } + proxies.mapValues { (name, _) -> getField(name) }
        val name = this@PhysicalComponentModel.name

        private val knownFieldsCount get(): Int =
            registeredFields.values.count { it != UNKNOWN } +
            registeredSubcomponents.values.flatten().sumBy { it.knownFieldsCount }

        init {
            for ((name, value) in fields) setField(name, value)
            for ((name, components) in subComponents) registerSubComponents(name, components)
            checkSubComponentsQuantities()
        }

        private fun checkSubComponentsQuantities() {
            for ((subComponentField, subComponents) in registeredSubcomponents) {
                val spec = this@PhysicalComponentModel.subcomponents.find { subComponentField == it.name }!!
                when {
                    spec.atMost == -1 -> require(subComponents.size >= spec.atLeast) { "Expected at least ${spec.atLeast} subcomponents under the name $subComponentField (${spec.type}), got ${subComponents.size}." }
                    spec.atLeast == spec.atMost -> require(subComponents.size == spec.atLeast) { "Expected exactly ${spec.atLeast} subcomponent(s) under the name $subComponentField (${spec.type}), got ${subComponents.size}." }
                    else -> require(subComponents.size in spec.atLeast..spec.atMost) { "Expected between ${spec.atLeast} and ${spec.atMost} subcomponents under the name $subComponentField (${spec.type}), got ${subComponents.size}." }
                }
            }
        }

        private fun setField(name: String, value: Any?) {
            if (name in proxies) setProxy(name, value)

            require(name in fields) { "Invalid field $name for component ${this.name}" }
            require(value == UNKNOWN || fields.getValue(name).isInstance(value)) { "Expected type ${fields.getValue(name).simpleName} for field $name, got $value"}
            registeredFields[name] = value
        }

        private fun setProxy(name: String, value: Any?) {
            val target = proxies.getValue(name)
            getSubComponents(target).single().setField(name, value)
        }

        private fun registerSubComponent(key: String, value: Instance) {
            val subComponentSpec = subcomponents.find { it.name == key }
            require(subComponentSpec != null) { "Invalid subcomponent name $key for component $name "}
            require(value.name == subComponentSpec.type) { "Invalid subcomponent type for field $key : expected ${subComponentSpec.type}, got ${value.name}" }
            registeredSubcomponents.getValue(key).add(value)
        }

        private fun registerSubComponents(key: String, values: List<Instance>) {
            for (value in values) {
                registerSubComponent(key, value)
            }
        }

        inline fun <reified T : Any> getField(fieldName: String): T? = getField(T::class, fieldName)
        fun <T : Any> getField(kClass: KClass<T>, fieldName: String): T? {
            if (fieldName in proxies) return getProxy(kClass, fieldName)

            require(fieldName in fields) { "Attempting to access non-existing field $fieldName of component $name"}
            require(fields.getValue(fieldName).isSubclassOf(kClass)) { "Cast from ${fields.getValue(fieldName).simpleName} to ${kClass.simpleName} for field $fieldName can't succeed"}
            @Suppress("UNCHECKED_CAST")
            return registeredFields.getValue(fieldName) as T?
        }

        private fun <T : Any> getProxy(kClass: KClass<T>, fieldName: String): T? {
            val target = proxies.getValue(fieldName)
            return getSubComponents(target).single().getField(kClass, fieldName)
        }

        fun getSubComponent(subComponentName: String, predicate: (Instance) -> Boolean = { true }): Instance? {
            return getSubComponents(subComponentName).find(predicate)
        }

        fun getSubComponents(subComponentsFieldName: String): List<Instance> {
            require(subComponentsFieldName in subcomponentsNames) { "Attempting to access non-existing subcomponent field $subComponentsFieldName of component $name"}
            return registeredSubcomponents.getValue(subComponentsFieldName)
        }

        fun hasSubComponent(subComponent: Instance): Boolean {
            val allSubComponents = registeredSubcomponents.values.flatten()
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
            return FormulaRegister.formulas.mapNotNull { it.computeAs<Any>(root, fieldName, this) }.firstOrNull()
        }

        private fun computeUnknownFieldsOfSubcomponents(root: RootPhysicalComponent) {
            registeredSubcomponents.values.forEach { subComponentList ->
                subComponentList.forEach { subComponent ->
                    subComponent.computeAll(root)
                    subComponent.computeUnknownFieldsOfSubcomponents(root)
                }
            }
        }

        fun typeOfSubComponent(subComponentsField: String) = subcomponents.find { it.name == subComponentsField }?.type
    }

}
