package physics.rules

import physics.components.Component


object RequirePreviousQueryDidNotMatch : QueryElement {
    override fun crawl(rootComponent: Component, resultsBases: Set<QueryResult>): Set<QueryResult> {
        if (resultsBases.isEmpty()) return setOf(QueryResult.empty())
        return emptySet()
    }
}