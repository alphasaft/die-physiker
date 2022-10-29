package physics.rules

import physics.components.Component

class Query(private vararg val elements: QueryElement) {
    fun crawl(rootComponent: Component, resultsBases: Set<QueryResult> = setOf(QueryResult.empty())): Set<QueryResult> {
        return elements.fold(resultsBases) { results, query -> query.crawl(rootComponent, resultsBases = results) }
    }
}
