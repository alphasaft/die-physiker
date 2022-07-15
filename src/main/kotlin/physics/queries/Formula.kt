package physics.queries

import physics.quantities.expressions.Equality


class Formula(
    private val name: String,
    private val equality: Equality,
) : Relation {
    constructor(equality: Equality): this(equality.toFlatString(), equality)

    override fun relateFieldsOf(queryResult: QueryResult) {
        val arguments = queryResult.asExpressionArguments()
        for (variable in equality.allVariables()) {
            val correspondingField = queryResult.getField(identifier = variable)
            correspondingField.setContent(equality.compute(variable, arguments = arguments))
        }
    }
}
