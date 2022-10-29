package physics.rules

import physics.components.Component

class UseStateEquation(
    private val identifier: String,
    private val sourceIdentifier: String,
    private val equationName: String,
) : QueryElement {
    override fun crawl(rootComponent: Component, resultsBases: Set<QueryResult>): Set<QueryResult> {
        val results = mutableSetOf<QueryResult>()
        for (base in resultsBases) {
            val source = base.getComponent(sourceIdentifier)
            val stateEquation = source.getStateEquation(equationName)
            if (!stateEquation.isKnown()) continue
            results.add(base.withAddedStateEquation(identifier, stateEquation))
        }
        return results
    }
}
