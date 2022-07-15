package physics.queries

import physics.components.Component

class ExtractBoxFields(
    val identifier: String,
    val sourceIdentifier: String,
    val fieldName: String
) : QueryElement {
    override fun crawl(rootComponent: Component, resultsBases: Set<QueryResult>): Set<QueryResult> {
        val results = mutableSetOf<QueryResult>()
        for (base in resultsBases) {
            val box = base.getBox(sourceIdentifier)
            val fields = box.map { component -> component.getField(fieldName) }
            results.add(base.withAddedBoxFields(identifier, fields))
        }
        return results
    }
}