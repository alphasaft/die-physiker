package physics.rules

import physics.components.Component

class WhenQueryReturns(private val query: Query) : RuleTrigger {
    override fun attachAndRun(rootComponent: Component, queryCallback: (QueryResult) -> Unit) {
        val results = query.crawl(rootComponent)
        if (results.any()) queryCallback(results.first())
    }
}
