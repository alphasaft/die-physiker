package physics.queries

import physics.components.Component

class ForEachQueryResult(private val query: Query) : RuleTrigger {
    override fun attachAndRun(rootComponent: Component, queryCallback: (QueryResult) -> Unit) {
        for (queryResult in query.crawl(rootComponent)) queryCallback(queryResult)
    }
}
