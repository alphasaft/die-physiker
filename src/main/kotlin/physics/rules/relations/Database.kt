package physics.rules.relations

import physics.quantities.PValue
import physics.quantities.asPValue
import physics.quantities.toQuantity
import physics.rules.QueryResult
import physics.rules.Relation

class Database(
    private val name: String,
    private val reader: DatabaseReader,
) : Relation {

    override fun relateFieldsOf(queryResult: QueryResult) {
        eachField@ for ((researchedVariable, researchedField) in queryResult.getNamedFields()) {
            val databaseQuery = mutableMapOf<String, String>()
            queryMaker@ for ((queryVariable, queryField) in queryResult.getNamedFields()) {
                if (queryVariable == researchedVariable) continue@queryMaker
                if (queryField.getContent() !is PValue<*>) continue@eachField

                databaseQuery[queryVariable] = queryField.getContent().asPValue().toString()
            }

            researchedField.addUpdate(trigger = name, reliesOn = queryResult.getAllFields() - researchedField) {
                researchedField.setContent(reader.select(researchedVariable, where = databaseQuery).toQuantity(researchedField.type))
            }
        }
    }
}
