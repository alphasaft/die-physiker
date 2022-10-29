package physics.rules

import physics.quantities.Quantity


class CustomRelation(
    vararg relationsPerOutputIdentifier: Pair<String, (args: QueryResult) -> Quantity<*>>
) : Relation {
    private val relationsPerOutputIdentifier = relationsPerOutputIdentifier.toMap()

    override fun relateFieldsOf(queryResult: QueryResult) {
        val fields = queryResult.getNamedFields()
        for ((identifier, field) in fields) {
            val fieldRelation = relationsPerOutputIdentifier[identifier] ?: continue
            field.setContent(fieldRelation(queryResult))
        }
    }
}