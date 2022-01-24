package physics.components

import physics.*
import physics.knowledge.Knowledge
import physics.reasoning.Goal
import physics.reasoning.Reasoning
import physics.reasoning.Result
import physics.reasoning.Step
import physics.quantities.Quantity
import titlecase
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


class ComponentClass(
    val name: String,
    val abstract: Boolean = false,
    val structure: ComponentStructure = ComponentStructure(),
    private val classCustomRepresentation: (Component) -> String = { throw ComponentException("Custom representation method was not implemented for class $name.") }
) {

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

        if (abstract) throw ComponentException("Can't instantiate abstract class $name.")
        try {
            instantiationBlock()
        } catch (e: ComponentException) {
            throw ComponentException("When instantiating $name : ${e.message}")
        }
    }

    operator fun invoke(
        componentName: String? = null,
        fieldValues: Map<String, Quantity<*>> = emptyMap(),
        subcomponentGroupsContents: Map<String, List<Component>> = emptyMap(),
    ): Instance {
        tryToInstantiate {
            fieldValues.keys.find { name -> structure.fieldsTemplates.none { it.name == name } }
                ?.also { throw ComponentException("$name(...) doesn't own the field '$it'") }
            subcomponentGroupsContents.keys.find { name -> structure.subcomponentsGroupsTemplates.none { it.name == name } }
                ?.also { throw ComponentException("$name(...) doesn't own a subcomponent group named '$it'") }

            val customRepresentation = if (componentName != null) {{ componentName }} else classCustomRepresentation
            val fields = structure.fieldsTemplates.map { it.newField(fieldValues.getValue(it.name)) }
            val subcomponents = structure.subcomponentsGroupsTemplates.map { it.newGroup(subcomponentGroupsContents[it.name] ?: emptyList())}
            val instance = Instance(customRepresentation, fields, subcomponents)

            fields.forEach { it.owner = instance }
            structure.init(instance)

            return instance
        }
    }

    inner class Instance internal constructor(
        private val customRepresentation: (Instance) -> String,
        val fields: List<Field<*>>,
        val subcomponentsGroups: List<Group>,
    ) {
        val componentClass = this@ComponentClass
        val className = componentClass.name

        fun getQuantity(fieldName: String): Quantity<*> {
            return getField(fieldName).getContent()
        }

        fun getField(fieldName: String): Field<*> {
            return fields.find { it.name == fieldName } ?: throw ComponentException("$className(...) doesn't own the field '$fieldName'")
        }

        fun getGroup(groupName: String): Group = subcomponentsGroups
            .find { it.name == groupName }
            ?: throw ComponentException("$className(...) doesn't own a subcomponent group named '$groupName'")

        operator fun contains(subcomponent: Component): Boolean = subcomponentsGroups.any { subcomponent in it }

        fun allSubcomponents(): List<Component> =
            subcomponentsGroups
                .map { it.content.map { c -> c.allSubcomponents() + c }.flatten() }
                .flatten()
        
        infix fun notInstanceOf(componentClass: ComponentClass) = !(this instanceOf componentClass)
        infix fun instanceOf(componentClass: ComponentClass): Boolean = this.componentClass inheritsOf componentClass

        fun fillField(
            name: String,
            knowledge: List<Knowledge>,
            context: Context = Context(this)
        ): Reasoning {
            val field = getField(name)
            val reasoning = Reasoning(Goal.GetFieldValue(field))

            for (knowledgeBit in knowledge) {
                try {
                    val fieldValue = knowledgeBit.getFieldValue(getField(name), context)
                    field.setContent(fieldValue)
                    reasoning.addStep(Step.UseKnowledge(knowledgeBit, knowledgeBit.toStringForGivenOutput(field, context)))
                } catch (e: InappropriateKnowledgeException) {
                    continue
                }
            }

            reasoning.result = Result.FieldValueComputed(field)
            return reasoning
        }

        operator fun invoke(modifier: ComponentModifier.() -> Unit) {
            ComponentModifier(this).apply(modifier)
        }

        fun isCustomRepresentationAvailable(): Boolean =
            try {
                customRepresentation(this)
                true
            } catch (e: ComponentException) {
                false
            }

        override fun toString(): String =
            if (isCustomRepresentationAvailable()) customRepresentation(this) else fullRepresentation()

        private fun fullRepresentation(): String {
            val newline = "\n    "
            val builder = StringBuilder()

            builder.append("${className.titlecase()}(")

            if (fields.isNotEmpty()) {
                fields.joinTo(
                    builder,
                    separator = newline,
                    prefix = newline,
                    postfix = if (subcomponentsGroups.isEmpty()) "\n" else newline
                ) { it.toString() }
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

            return builder.toString()
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
