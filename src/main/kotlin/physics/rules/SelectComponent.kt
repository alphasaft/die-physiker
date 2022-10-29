package physics.rules

import physics.components.Component
import physics.components.ComponentClass

class SelectComponent(
    private val identifier: String,
    private val sourceIdentifier: String,
    private val boxName: String,
    private val type: ComponentClass = ComponentClass.Any
) : QueryElement {
    override fun crawl(rootComponent: Component, resultsBases: Set<QueryResult>): Set<QueryResult> {
        val results = mutableSetOf<QueryResult>()
        for (base in resultsBases) {
            val source = base.getComponent(sourceIdentifier)
            val box = source.getBox(boxName)
            for (component in box.filter { it instanceOf type }) {
                results.add(base.withAddedComponent(identifier, component))
            }
        }
        return results
    }
}