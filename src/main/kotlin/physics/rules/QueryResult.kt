package physics.rules

import ensureOrElse
import physics.components.*
import physics.quantities.*
import physics.quantities.expressions.VariableValue
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

data class QueryResult(
    private val components: Map<String, Component>,
    private val boxes: Map<String, ComponentBox>,
    private val fields: Map<String, Field<*>>,
    private val boxFields: Map<String, List<Field<*>>>,
    private val equations: Map<String, StateEquation>,
) {

    companion object {
        fun empty() = QueryResult(
            emptyMap(),
            emptyMap(),
            emptyMap(),
            emptyMap(),
            emptyMap()
        )
    }
    
    fun getComponent(identifier: String): Component =
        components[identifier] ?: throw IllegalArgumentException("Invalid component identifier $identifier.")

    fun getBox(identifier: String): ComponentBox =
        boxes[identifier] ?: throw IllegalArgumentException("Invalid box identifier $identifier.")

    fun getField(identifier: String): Field<*> =
        fields[identifier] ?: throw IllegalArgumentException("Invalid field identifier $identifier.")

    fun getBoxFields(identifier: String): List<Field<*>> =
        boxFields[identifier] ?: throw IllegalArgumentException("Invalid box fields identifier $identifier.")

    fun getStateEquation(identifier: String): StateEquation =
        equations[identifier] ?: throw java.lang.IllegalArgumentException("Invalid equation identifier $identifier.")

    fun getNamedFields() =
        fields

    fun getAllFields() =
        fields.values + boxFields.values.flatten()

    fun asExpressionArguments(): Map<String, VariableValue<*>> {
        return (
                fields.mapValues { (_, f) -> VariableValue.Single(f.getContent().toQuantity<PDouble>()) }
                + boxFields.mapValues { (_, b) -> VariableValue.Array(b.map { it.getContent().toQuantity<PDouble>() }) }
        )
    }

    fun withAddedComponent(identifier: String, component: Component): QueryResult =
        copy(components = components + Pair(identifier, component))

    fun withAddedBox(identifier: String, box: ComponentBox): QueryResult =
        copy(boxes = boxes + Pair(identifier, box))

    fun withAddedField(identifier: String, field: Field<*>): QueryResult =
        copy(fields = fields + Pair(identifier, field))

    fun withAddedBoxFields(identifier: String, fields: List<Field<*>>): QueryResult =
        copy(boxFields = boxFields + Pair(identifier, fields))

    fun withAddedStateEquation(identifier: String, equation: StateEquation): QueryResult =
        copy(equations = equations + Pair(identifier, equation))

    fun allCollectedFields(): List<Field<*>> {
        return fields.values + boxFields.values.flatten()
    }


    private class ShapeError : Error()
    inner class ArgumentsShapeBuilder {

        @PublishedApi
        internal val shapeError = { throw ShapeError() }

        inline fun <reified T : Any> plainValue(fieldName: String): T {
            val fieldContent = getField(fieldName).getContent().asPValueOrElse(shapeError)
            return fieldContent.value.ensureOrElse<T>(shapeError)
        }

        inline fun <reified T : PValue<T>> pValue(fieldName: String): T {
            val fieldContent = getField(fieldName).getContent().asPValueOrElse(shapeError)
            return fieldContent.toPValue<T>()
        }

        inline fun <reified T : PValue<T>> quantity(fieldName: String): Quantity<T> {
            val fieldContent = getField(fieldName).getContent()
            return fieldContent.toQuantity<T>()
        }

        inline fun <reified T : Any> plainValues(boxFieldsName: String): List<T> {
            val fieldsContents = getBoxFields(boxFieldsName).map { it.getContent().asPValueOrElse(shapeError) }
            return fieldsContents.map { it.value.ensureOrElse<T>(shapeError) }
        }

        inline fun <reified T : PValue<T>> pValues(boxFieldsName: String): List<T> {
            val fieldsContents = getBoxFields(boxFieldsName).map { it.getContent().asPValueOrElse(shapeError) }
            return fieldsContents.map { it.toPValue<T>() }
        }

        inline fun <reified T : PValue<T>> quantities(boxFieldsName: String): List<Quantity<T>> {
            val fieldsContents = getBoxFields(boxFieldsName).map { it.getContent() }
            return fieldsContents.map { it.toQuantity<T>() }
        }

        fun component(componentName: String, type: ComponentClass = ComponentClass.Any): Component {
            val component = getComponent(componentName)
            if (component notInstanceOf type) shapeError()
            return component
        }
    }

    inline fun requireShape(onError: () -> Nothing, shape: ArgumentsShapeBuilder.() -> Unit) {
        contract {
            callsInPlace(shape, InvocationKind.EXACTLY_ONCE)
        }

        try {
            ArgumentsShapeBuilder().apply(shape)
        } catch (e: ShapeError) {
            onError()
        }
    }
}