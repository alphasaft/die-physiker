package physics.rules

import physics.quantities.Quantity

class ComputeField(private val fieldName: String, val computation: (QueryResult) -> Quantity<*>) : Action {
    override fun execute(queryResult: QueryResult) {
        val computedField = queryResult.getField(fieldName)
        computedField.setContent(computation(queryResult))
    }

}