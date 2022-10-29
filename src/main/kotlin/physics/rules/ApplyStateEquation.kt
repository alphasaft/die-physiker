package physics.rules

class ApplyStateEquation(private val stateEquationIdentifier: String) : Action {
    override fun execute(queryResult: QueryResult) {
        val stateEquation = queryResult.getStateEquation(stateEquationIdentifier)
        return stateEquation.toFormula().relateFieldsOf(queryResult)
    }
}
