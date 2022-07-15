package physics.queries

import physics.components.Component

interface QueryElement {
    fun crawl(rootComponent: Component, resultsBases: Set<QueryResult>): Set<QueryResult>
}
