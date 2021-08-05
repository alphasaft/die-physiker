package physics.components

import physics.ComponentGroupNotFoundException
import physics.FieldCastException
import physics.FieldNotFoundException
import physics.titlecase
import physics.values.PhysicalValue

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


class ComponentClass(
    val name: String,
    extends: List<ComponentClass> = emptyList(),
    fieldsTemplates: List<Field.Template<*>> = emptyList(),
    subcomponentsGroupsTemplates: List<ComponentGroup.Template> = emptyList(),
) {
    private val bases = extends
    private val allBases: List<ComponentClass> = bases + bases.map { it.allBases }.flatten()
    private val fieldsTemplates: List<Field.Template<*>> = fieldsTemplates + bases.map { it.fieldsTemplates }.flatten()
    private val subcomponentsGroupsTemplates: List<ComponentGroup.Template> = subcomponentsGroupsTemplates + bases.map { it.subcomponentsGroupsTemplates }.flatten()

    inner class Instance internal constructor(
        val fields: List<Field<*>>,
        private val subcomponentsGroups: List<ComponentGroup>,
    ) {
        val componentClass = this@ComponentClass
        val name = componentClass.name

        @JvmName("getFieldAsPhysicalValue")
        fun getField(fieldName: String) = getField<PhysicalValue<*>>(fieldName)
        inline fun <reified T : PhysicalValue<*>> getField(fieldName: String): Field<T> = getField(fieldName, T::class)
        fun <T : PhysicalValue<*>> getField(fieldName: String, kClass: KClass<T>): Field<T> {
            val field = fields.find { it.name == fieldName } ?: throw FieldNotFoundException(fieldName, name)
            if (!field.type.isSubclassOf(kClass)) throw FieldCastException(field, kClass)
            return (@Suppress("UNCHECKED_CAST") (field as Field<T>))
        }

        inline fun <reified T : PhysicalValue<*>> get(fieldName: String): T = get(fieldName, T::class)
        fun <T : PhysicalValue<*>> get(fieldName: String, kClass: KClass<T>): T {
            return getField(fieldName, kClass).getContent()
        }

        inline fun <reified T : PhysicalValue<*>> getOrNull(fieldName: String): T? = getOrNull(fieldName, T::class)
        fun <T : PhysicalValue<*>> getOrNull(fieldName: String, kClass: KClass<T>): T? {
            return getField(fieldName, kClass).getContentOrNull()
        }

        fun hasSubcomponentGroup(groupName: String): Boolean = subcomponentsGroups.any { it.name == groupName }

        fun getSubcomponentGroup(groupName: String): ComponentGroup = subcomponentsGroups
            .find { it.name == groupName }
            ?: throw ComponentGroupNotFoundException(groupName, name)

        infix fun instanceOf(componentClass: ComponentClass) = this.componentClass.inheritsOf(componentClass)
        infix fun notInstanceOf(componentClass: ComponentClass) = !(this instanceOf componentClass)

        fun allSubcomponents(): List<Component> =
            subcomponentsGroups
                .map { it.content.map { c -> c.allSubcomponents() + c }.flatten() }
                .flatten()

        operator fun contains(subcomponent: Component): Boolean =
            subcomponentsGroups.any { subcomponent in it }

        override fun toString(): String {
            val newline = "\n    "
            val builder = StringBuilder()

            builder.append("${name.titlecase()}(")

            if (fields.isNotEmpty()) {
                fields.joinTo(
                    builder,
                    separator = newline,
                    prefix = newline,
                    postfix = if (subcomponentsGroups.isEmpty()) "\n" else newline
                )
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

    operator fun invoke(
        fieldValuesAsStrings: Map<String, String> = emptyMap(),
        subcomponentGroupsContents: Map<String, List<Component>> = emptyMap(),
    ): Instance {
        fieldValuesAsStrings.keys.find { name -> fieldsTemplates.none { it.name == name } }?.also { throw FieldNotFoundException(it, name) }
        subcomponentGroupsContents.keys.find { name -> subcomponentsGroupsTemplates.none { it.name == name } }?.also { throw ComponentGroupNotFoundException(it, name) }

        val fields = fieldsTemplates.map { it.newField(fieldValuesAsStrings[it.name]) }
        val subcomponents = subcomponentsGroupsTemplates.map { it.newGroup(subcomponentGroupsContents[it.name] ?: emptyList()) }
        return Instance(fields, subcomponents)
    }

    infix fun inheritsOf(componentClass: ComponentClass): Boolean {
        return componentClass == this || componentClass in allBases
    }

    override fun toString(): String {
        return "component class $name"
    }
}
