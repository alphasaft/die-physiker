package physics.rules

import physics.components.Component


class AddCondition(
    private val identifier: String,
    private val condition: (QueryResult) -> Boolean,
) : QueryElement {
    override fun crawl(rootComponent: Component, resultsBases: Set<QueryResult>): Set<QueryResult> {
        return resultsBases.filterTo(mutableSetOf(), condition)
    }
}