package physics.queries

import physics.components.Component

class Rule(
    private val trigger: RuleTrigger,
    private vararg val actions: Action,
) {
    infix fun then(action: Action): Rule = Rule(trigger, *actions, action)

    fun applyOn(rootComponent: Component) {
        trigger.attachAndRun(rootComponent) {
            for (action in actions) {
                action.execute(it)
            }
        }
    }
}
