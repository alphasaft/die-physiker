package physics.rules

import physics.components.Component


class ExtractField(
    private val identifier: String,
    private val sourceIdentifier: String,
    private val fieldName: String,
) : QueryElement {
    override fun crawl(rootComponent: Component, resultsBases: Set<QueryResult>): Set<QueryResult> {
        val results = mutableSetOf<QueryResult>()
        for (base in resultsBases) {
            val source = base.getComponent(sourceIdentifier)
            val field = source.getField(fieldName)
            results.add(base.withAddedField(identifier, field))
        }
        return results
    }
}