package physics.queries

import Predicate
import physics.components.Component
import physics.components.Field


class AddCondition(
    private val identifier: String,
    private val condition: (QueryResult) -> Boolean,
) : QueryElement {
    override fun crawl(rootComponent: Component, resultsBases: Set<QueryResult>): Set<QueryResult> {
        return resultsBases.filterTo(mutableSetOf(), condition)
    }
}