package physics.rules

import physics.components.Component

interface RuleTrigger {
    infix fun pleaseDo(action: Action): Rule = Rule(this, action)
    fun attachAndRun(rootComponent: Component, queryCallback: (QueryResult) -> Unit)
}
