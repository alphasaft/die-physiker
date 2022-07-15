package loaders


import loaders.base.Ast
import loaders.base.DataLoader
import Args
import physics.components.Component
import physics.components.ComponentClass
import physics.quantities.Quantity


/*
class ActionsLoader(
    loadedComponentClasses: Map<String, ComponentClass>,
    private val functionsRegister: FunctionsRegister
) : DataLoader<ActionsParser, Action>(ActionsParser) {
    private val specLoader = RequirementLoader(loadedComponentClasses, functionsRegister)

    class FunctionsRegister internal constructor() : RequirementLoader.FunctionsRegister() {
        private val actionImplementations = mutableMapOf<String, (Args<Component>, Args<Quantity<*>>) -> Unit>()

        fun addComponentModifier(actionRef: String, actionImpl: (Args<Component>, Args<Quantity<*>>) -> Unit) {
            actionImplementations[actionRef] = actionImpl
        }

        fun getComponentModifier(actionRef: String): (Args<Component>, Args<Quantity<*>>) -> Unit {
            return actionImplementations.getValue(actionRef)
        }
    }

    companion object {
        fun getFunctionRegister() = FunctionsRegister()
    }


    override fun generateFrom(ast: Ast): Nothing {
        /*
        val name = ast["reactionName"]
        val specs = ComponentsPicker((ast.."specs").allNodes("spec-#").map { specLoader.generateFrom(it.toAst()) })
        val action = functionsRegister.getComponentModifier(ast["actionFunctionRef"])
        return Action(
            name,
            specs,
            action
        )
    }
}

 */