package physics.components

import physics.*
import physics.quantities.AnyQuantity
import physics.quantities.Quantity
import physics.quantities.expressions.Equation
import physics.rules.Rule
import println
import titlecase
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


class ComponentClass(
    val name: String,
    val abstract: Boolean = false,
    val structure: ComponentStructure = ComponentStructure(),
    val rules: Set<Rule> = emptySet(),
) {

    companion object {
        @Suppress("PropertyName")
        val Any = ComponentClass("Any", abstract = true)
    }

    override fun toString(): String {
        return "component class $name"
    }

    infix fun inheritsOf(componentClass: ComponentClass): Boolean {
        return componentClass == this || componentClass == Any || componentClass in structure.bases
    }

    private inline fun tryToInstantiate(componentName: String, instantiationBlock: () -> Unit) {
        contract {
            callsInPlace(instantiationBlock, InvocationKind.EXACTLY_ONCE)
        }

        if (abstract) throw ComponentException("Can't instantiate abstract class $name.")
        try {
            instantiationBlock()
        } catch (e: ComponentException) {
            throw ComponentException("When instantiating $componentName : ${e.message}")
        }
    }

    operator fun invoke(
        componentName: String,
        fieldValues: Map<String, Quantity<*>> = emptyMap(),
        boxesContents: Map<String, List<Component>> = emptyMap(),
        stateEquations: Map<String, Equation> = emptyMap()
    ): Instance {
        tryToInstantiate(componentName) {
            fieldValues.keys.find { it !in structure.fieldsTemplates }
                ?.also { throw ComponentException("$name(...) has no field '$it'.") }
            boxesContents.keys.find { it !in structure.boxesTemplates }
                ?.also { throw ComponentException("$name(...) has no box named '$it'.") }
            stateEquations.keys.find { it !in structure.equationsTemplates }
                ?.also { throw ComponentException("$name(...) has no state equation named '$it'.") }

            val fields = structure.fieldsTemplates.mapValues { (n, t) -> t.create(fieldValues[n] ?: AnyQuantity(t.type), componentName) }
            val subcomponents = structure.boxesTemplates.mapValues { (n, t) -> t.create(boxesContents[n] ?: emptyList())}
            val equations = structure.equationsTemplates.mapValues { (n, t) -> t.create(stateEquations.getValue(n)) }
            val instance = Instance(componentName, fields, subcomponents, equations)

            structure.init(instance)

            return instance
        }
    }

    inner class Instance internal constructor(
        val name: String,
        val fields: Map<String, Field<*>>,
        val boxes: Map<String, ComponentBox>,
        private val stateEquations: Map<String, StateEquation>
    ) {
        val componentClass = this@ComponentClass
        val className = componentClass.name

        fun update() {
            do {
                val hash = hashCode()
                for (rule in rules) {
                    rule.applyOn(this)
                }
            } while (hash != hashCode())
        }

        fun getQuantity(fieldName: String): Quantity<*> {
            return getField(fieldName).getContent()
        }

        fun getField(fieldName: String): Field<*> {
            return fields[fieldName] ?: throw ComponentException("$className(...) doesn't own the field '$fieldName'")
        }

        fun getBox(boxName: String): ComponentBox {
            return boxes[boxName] ?: throw ComponentException("$className(...) doesn't own a box named '$boxName'")
        }

        fun getStateEquation(equationName: String): StateEquation {
            return stateEquations[equationName] ?: throw ComponentException("$className(...) doesn't own a state equation named '$equationName'.")
        }

        fun allSubcomponents(): List<Component> {
            return boxes
                .values
                .map { it.content.map { c -> c.allSubcomponents() + c }.flatten() }
                .flatten()
        }

        operator fun invoke(modifier: ComponentModifier.() -> Unit) {
            ComponentModifier(this).apply(modifier)
        }

        infix fun notInstanceOf(componentClass: ComponentClass) = !(this instanceOf componentClass)
        infix fun instanceOf(componentClass: ComponentClass): Boolean = this.componentClass inheritsOf componentClass

        operator fun contains(component: Component): Boolean {
            return boxes.values.any { component in it }
        }

        override fun toString(): String {
            return name ?: fullRepresentation()
        }

        fun fullRepresentation(): String {
            // TODO : Add state equations to representation.
            val newline = "\n    "
            val builder = StringBuilder()

            builder.append("${className.titlecase()}(")

            if (fields.isNotEmpty()) {
                fields.values.joinTo(
                    builder,
                    separator = newline,
                    prefix = newline,
                    postfix = if (boxes.isEmpty() && stateEquations.isEmpty()) "\n" else ""
                ) { it.toStringWithHistory() }
            }

            if (boxes.isNotEmpty()) {
                builder.append(
                    boxes.values.joinToString(
                        separator = newline,
                        prefix = newline,
                        postfix = if (stateEquations.isEmpty()) "\n" else ""
                    ) { it.toString().replace("\n", newline) }
                )
            }

            if (stateEquations.isNotEmpty()) {
                builder.append(
                    stateEquations.toList().joinToString(
                        separator = newline,
                        prefix = newline,
                        postfix = "\n"
                    ) { (n, e) -> "($n) : $e".replace("\n", newline) }
                )
            }

            builder.append(")")

            return builder.toString()
        }

        override fun equals(other: Any?): Boolean {
            return other is Component && other.fields == fields && other.boxes == boxes
        }

        override fun hashCode(): Int {
            var result = fields.hashCode()
            result = 31 * result + boxes.hashCode()
            result = 31 * result + componentClass.hashCode()
            return result
        }
    }
}
