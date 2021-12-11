package loaders


import loaders.base.Ast
import loaders.base.DataLoader
import physics.Args
import physics.components.Component
import physics.components.ComponentClass
import physics.components.RequirementsHandler
import physics.dynamic.Action
import physics.values.PhysicalValue


class ActionsLoader(
    loadedComponentClasses: Map<String, ComponentClass>,
    private val functionsRegister: FunctionsRegister
) : DataLoader<ActionsParser, Action>(ActionsParser) {
    private val requirementLoader = RequirementLoader(loadedComponentClasses, functionsRegister)

    class FunctionsRegister internal constructor() : RequirementLoader.FunctionsRegister() {
        private val actionImplementations = mutableMapOf<String, (Args<Component>, Args<PhysicalValue<*>>) -> Unit>()

        fun addComponentModifier(actionRef: String, actionImpl: (Args<Component>, Args<PhysicalValue<*>>) -> Unit) {
            actionImplementations[actionRef] = actionImpl
        }

        fun getComponentModifier(actionRef: String): (Args<Component>, Args<PhysicalValue<*>>) -> Unit {
            return actionImplementations.getValue(actionRef)
        }
    }

    companion object {
        fun getFunctionRegister() = FunctionsRegister()
    }

    override fun generateFrom(ast: Ast): Action {
        val name = ast["reactionName"]
        val requirements = RequirementsHandler((ast.."requirements").allNodes("requirement-#").map { requirementLoader.generateFrom(it.toAst()) })
        val action = functionsRegister.getComponentModifier(ast["actionFunctionRef"])
        return Action(
            name,
            requirements,
            action
        )
    }
}