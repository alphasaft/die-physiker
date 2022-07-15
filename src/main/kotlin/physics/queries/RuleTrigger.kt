package physics.queries

import physics.components.Component

interface RuleTrigger {
    infix fun perform(action: Action): Rule = Rule(this, action)
    fun attachAndRun(rootComponent: Component, queryCallback: (QueryResult) -> Unit)
}
