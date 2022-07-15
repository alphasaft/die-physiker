package physics.queries

import physics.components.*
import physics.quantities.PReal
import physics.quantities.castAs
import physics.quantities.expressions.VariableValue

data class QueryResult(
    private val components: Map<String, Component>,
    private val boxes: Map<String, ComponentBox>,
    val singleFields: Map<String, Field<*>>,
    private val boxFields: Map<String, List<Field<*>>>,
) {
    companion object {
        fun empty() = QueryResult(
            emptyMap(),
            emptyMap(),
            emptyMap(),
            emptyMap()
        )
    }

    private val usedIdentifiers = (components + boxes + singleFields + boxFields).keys

    private fun getUnusedIdentifier(identifier: String): String {
        if (identifier !in usedIdentifiers) return identifier
        var i = 2
        while ("$identifier$i" in identifier) i++
        return "$identifier$i"
    }

    
    fun getComponent(identifier: String): Component =
        components[identifier] ?: throw IllegalArgumentException("Invalid component identifier $identifier.")

    fun getBox(identifier: String): ComponentBox =
        boxes[identifier] ?: throw IllegalArgumentException("Invalid box identifier $identifier.")

    fun getField(identifier: String): Field<*> =
        singleFields[identifier] ?: throw IllegalArgumentException("Invalid field identifier $identifier.")

    fun getBoxFields(identifier: String): List<Field<*>> =
        boxFields[identifier] ?: throw IllegalArgumentException("Invalid box fields identifier $identifier.")

    fun asExpressionArguments(): Map<String, VariableValue<*>> {
        return (
                singleFields.mapValues { (_, f) -> VariableValue.Single(f.getContent().castAs<PReal>()) }
                + boxFields.mapValues { (_, b) -> VariableValue.Array(b.map { it.getContent().castAs<PReal>() }) }
        )
    }

    fun withAddedComponent(identifier: String, component: Component): QueryResult =
        copy(components = components + Pair(getUnusedIdentifier(identifier), component))

    fun withAddedBox(identifier: String, box: ComponentBox): QueryResult =
        copy(boxes = boxes + Pair(getUnusedIdentifier(identifier), box))

    fun withAddedField(identifier: String, field: Field<*>): QueryResult =
        copy(singleFields = singleFields + Pair(getUnusedIdentifier(identifier), field))

    fun withAddedBoxFields(identifier: String, fields: List<Field<*>>): QueryResult =
        copy(boxFields = boxFields + Pair(getUnusedIdentifier(identifier), fields))
}