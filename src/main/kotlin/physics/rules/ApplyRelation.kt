package physics.rules

class ApplyRelation(private val relation: Relation) : Action {
    override fun execute(queryResult: QueryResult) {
        relation.relateFieldsOf(queryResult)
    }
}
