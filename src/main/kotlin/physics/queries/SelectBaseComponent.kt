package physics.queries

import physics.components.*

class SelectBaseComponent(
    val identifier: String,
    val type: ComponentClass,
) : QueryElement {
    override fun crawl(rootComponent: Component, resultsBases: Set<QueryResult>): Set<QueryResult> {
        val results = mutableSetOf<QueryResult>()
        for (base in resultsBases) {
            for (component in rootComponent.allSubcomponents() + rootComponent) {
                if (component instanceOf type) results.add(base.withAddedComponent(identifier, component))
            }
        }
        return results
    }
}