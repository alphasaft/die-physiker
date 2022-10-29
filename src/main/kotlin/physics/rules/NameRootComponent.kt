package physics.rules

import physics.components.*

class NameRootComponent(
    val identifier: String,
) : QueryElement {
    override fun crawl(rootComponent: Component, resultsBases: Set<QueryResult>): Set<QueryResult> {
        return resultsBases.map { it.withAddedComponent(identifier, rootComponent) }.toSet()
    }
}
