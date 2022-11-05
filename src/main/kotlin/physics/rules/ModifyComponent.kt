package physics.rules

import physics.components.ComponentModifier

class ModifyComponent(private val componentName: String, private val modifier: ComponentModifier.() -> Unit) : Action {
    override fun execute(queryResult: QueryResult) {
        val component = queryResult.getComponent(componentName)
        component.invoke(modifier)
    }
}
