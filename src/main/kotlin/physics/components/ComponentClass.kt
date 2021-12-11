package physics.components

import physics.*
import physics.computation.BasePhysicalKnowledge
import physics.dynamic.Action
import physics.dynamic.Behavior
import physics.values.PhysicalValue
import println
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


class ComponentClass(
    val name: String,
    val abstract: Boolean = false,
    val structure: ComponentStructure = ComponentStructure(),
    representationField: String? = null,
) : ComponentClassifier {
    private val representationField: String? = representationField ?: structure.bases.firstOrNull()?.representationField

    init {
        if (representationField != null) {
            require(representationField.isNotBlank()) { "Representation field shouldn't be blank." }
            require(representationField in structure.fieldsNames) { "Chosen representation field '$representationField' isn't an existing field. " }
        }
    }

    override fun toString(): String {
        return "component class $name"
    }

    infix fun inheritsOf(componentClass: ComponentClass): Boolean {
        return componentClass == this || componentClass in structure.bases
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun tryToInstantiate(instantiationBlock: () -> Unit) {
        contract {
            callsInPlace(instantiationBlock, InvocationKind.EXACTLY_ONCE)
        }

        if (abstract) throw AbstractComponentInstantiationError(name)
        try {
            instantiationBlock()
        } catch (e: ComponentException) {
            throw ComponentInstantiationError(name, e)
        }
    }

    operator fun invoke(
        fieldValuesAsStrings: Map<String, String> = emptyMap(),
        subcomponentGroupsContents: Map<String, List<Component>> = emptyMap(),
        behaviorsImplementations: Map<String, Action> = emptyMap()
    ): Instance {
        tryToInstantiate {
            fieldValuesAsStrings.keys.find { name -> structure.fieldsTemplates.none { it.name == name } }
                ?.also { throw FieldNotFoundException(it, name) }
            subcomponentGroupsContents.keys.find { name -> structure.subcomponentsGroupsTemplates.none { it.name == name } }
                ?.also { throw ComponentGroupNotFoundException(it, name) }
            behaviorsImplementations.keys.find { name -> structure.behaviorsTemplates.none { it.name == name } }
                ?.also { throw BehaviorNotFoundException(it, name) }

            val fields = structure.fieldsTemplates.map { it.newField(fieldValuesAsStrings[it.name]) }
            val subcomponents = structure.subcomponentsGroupsTemplates.map { it.newGroup(subcomponentGroupsContents[it.name] ?: emptyList())}
            val behaviors = structure.behaviorsTemplates.map { it.newBehavior(behaviorsImplementations[it.name]) }
            return Instance(fields, subcomponents, behaviors)
        }
    }

    inner class Instance internal constructor(
        val fields: List<Field<*>>,
        val subcomponentsGroups: List<ComponentGroup>,
        private val behaviors: List<Behavior>
    ) {

        val componentClass = this@ComponentClass
        val name = componentClass.name
        private val knownFieldsCount: Int get() = fields.count { it.isKnown() } + subcomponentsGroups.sumOf { g -> g.content.sumOf { c -> c.knownFieldsCount } }

        init {
            update()
        }

        @JvmName("getFieldAsPhysicalValue")
        fun getField(fieldName: String) = getField(fieldName, PhysicalValue::class)
        private fun <T : PhysicalValue<*>> getField(fieldName: String, kClass: KClass<T>): Field<T> {
            val field = fields.find { it.name == fieldName } ?: throw FieldNotFoundException(fieldName, name)
            if (!field.type.isSubclassOf(kClass)) throw FieldCastException(field, kClass)
            return (@Suppress("UNCHECKED_CAST") (field as Field<T>))
        }

        inline operator fun <reified T : PhysicalValue<*>> get(fieldName: String): T = get(fieldName, T::class)
        fun <T : PhysicalValue<*>> get(fieldName: String, kClass: KClass<T>): T {
            return getField(fieldName, kClass).getContent()
        }

        inline fun <reified T : PhysicalValue<*>> getOrNull(fieldName: String): T? = getOrNull(fieldName, T::class)
        fun <T : PhysicalValue<*>> getOrNull(fieldName: String, kClass: KClass<T>): T? {
            return getField(fieldName, kClass).getContentOrNull()
        }

        fun getSubcomponentGroup(groupName: String): ComponentGroup = subcomponentsGroups
            .find { it.name == groupName }
            ?: throw ComponentGroupNotFoundException(groupName, name)

        fun allSubcomponents(): List<Component> =
            subcomponentsGroups
                .map { it.content.map { c -> c.allSubcomponents() + c }.flatten() }
                .flatten()

        infix fun instanceOf(componentClassifier: ComponentClassifier): Boolean {
            return when (componentClassifier) {
                is ComponentClass -> this.componentClass inheritsOf componentClassifier
                is ComponentClassForwardRef -> this.componentClass.name == componentClassifier.className
            }
        }

        infix fun notInstanceOf(componentClass: ComponentClassifier) = !(this instanceOf componentClass)
        operator fun contains(subcomponent: Component): Boolean = subcomponentsGroups.any { subcomponent in it }

        fun fillFieldsWithTheirValuesUsing(
            knowledge: List<BasePhysicalKnowledge>,
            system: PhysicalSystem = PhysicalSystem(this)
        ) {
            var oldKnownFieldsCount = knownFieldsCount
            while (true) {
                for (anyKnowledge in knowledge) for (field in fields) {
                    if (field.isKnown()) continue
                    try { anyKnowledge.fillFieldWithItsValue(field, system) }
                    catch (e: InappropriateKnowledgeException) { continue }
                    break
                }

                for (group in subcomponentsGroups) for (component in group.content) {
                    component.fillFieldsWithTheirValuesUsing(knowledge, system)
                }

                if (knownFieldsCount == oldKnownFieldsCount) break
                oldKnownFieldsCount = knownFieldsCount
                update()
            }
        }

        private fun update() {
            for (behavior in behaviors) {
                behavior.applyRepetitivelyOn(this)
            }
        }

        operator fun invoke(modifier: ComponentModifier.() -> Unit) {
            ComponentModifier(this).apply(modifier)
        }

        override fun toString(): String {
            return if (representationField != null && getOrNull<PhysicalValue<*>>(representationField) != null) toStringCustom()!!
            else toStringDefault()
        }

        fun toStringCustom(): String? {
            if (representationField == null) return null
            return getOrNull<PhysicalValue<*>>(representationField)?.toString()
        }

        fun toStringDefault(): String {
            val newline = "\n    "
            val builder = StringBuilder()

            builder.append("${name.titlecase()}(")

            if (fields.isNotEmpty()) {
                fields.joinTo(
                    builder,
                    separator = newline,
                    prefix = newline,
                    postfix = if (subcomponentsGroups.isEmpty()) "\n" else newline
                ) { it.toString(owner = this) }
            }

            if (subcomponentsGroups.isNotEmpty()) {
                builder.append(
                    subcomponentsGroups.joinToString(
                        separator = newline,
                        postfix = "\n"
                    ) { it.toString().replace("\n", newline) }
                )
            }

            builder.append(")")

            return builder.toString().replace("\n", "\n")
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Component) return false
            return other.fields == fields && other.subcomponentsGroups == subcomponentsGroups
        }

        override fun hashCode(): Int {
            var result = fields.hashCode()
            result = 31 * result + subcomponentsGroups.hashCode()
            result = 31 * result + componentClass.hashCode()
            return result
        }
    }
}
