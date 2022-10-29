package physics.rules

import physics.components.Component

class SelectBox(
    private val identifier: String,
    private val sourceIdentifier: String,
    private val boxName: String,
) : QueryElement {
    override fun crawl(rootComponent: Component, resultsBases: Set<QueryResult>): Set<QueryResult> {
        val results = mutableSetOf<QueryResult>()
        for (base in resultsBases) {
            val source = base.getComponent(sourceIdentifier)
            val box = source.getBox(boxName)
            results.add(base.withAddedBox(identifier, box))
        }
        return results
    }
}
