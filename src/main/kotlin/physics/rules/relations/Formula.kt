package physics.rules.relations

import physics.quantities.expressions.Equation
import physics.rules.QueryResult
import physics.rules.Relation


class Formula(
    private val equation: Equation,
) : Relation {

    override fun relateFieldsOf(queryResult: QueryResult) {
        val arguments = queryResult.asExpressionArguments()
        for ((variable, field) in queryResult.getNamedFields()) {
            val value = equation.compute(variable, arguments)
            val fieldsUsed = queryResult.getAllFields() - field
            field.addUpdate(trigger = equation.isolateVariable(variable).toFlatString(), reliesOn = fieldsUsed) {
                field.setContent(value)
            }
        }
    }
}
