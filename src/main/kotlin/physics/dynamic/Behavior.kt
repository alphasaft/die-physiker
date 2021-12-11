package physics.dynamic

import physics.Args
import physics.MissingBehaviorImplException
import physics.components.Component
import physics.components.RequirementsHandler
import physics.values.PhysicalValue


class Behavior(private val underlyingAction: Action) {
    val name get() = underlyingAction.name

    class Template private constructor(
        val name: String,
        private val underlyingAction: Action?,
    ) {
        companion object {
            fun dynamic(name: String) = Template(name = name, underlyingAction = null)
            fun static(name: String, requirementsHandler: RequirementsHandler, behaviorImpl: (Args<Component>, Args<PhysicalValue<*>>) -> Unit) =
                Template(name, Action(
                    name,
                    requirementsHandler,
                    behaviorImpl
                ))
        }

        fun newBehavior(underlyingAction: Action? = null): Behavior {
            require(underlyingAction == null || name == underlyingAction.name) { "Expected an action named '$name', got '${underlyingAction!!.name}'" }

            if (underlyingAction == null && this.underlyingAction == null) throw MissingBehaviorImplException("No action was provided for dynamic behavior '$name'.")
            if (underlyingAction != null && this.underlyingAction != null) throw MissingBehaviorImplException("A static behavior cannot be implemented separately by the instances of the component class.")
            return Behavior(underlyingAction ?: this.underlyingAction!!)
        }
    }

    fun applyOn(that: Component) {
        return underlyingAction.applyOn(that)
    }

    fun applyRepetitivelyOn(that: Component) {
        return underlyingAction.applyRepetitivelyOn(that)
    }
}
