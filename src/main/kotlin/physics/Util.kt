package physics

import physics.specs.ComponentSpec
import physics.specs.FieldAccessSpec
import physics.specs.RootComponentSpec
import kotlin.reflect.KClass


val UNKNOWN = null

internal val builtinNamesToClasses: Map<String, KClass<*>> = listOf(
    Int::class,
    String::class,
    Char::class,
    Float::class,
    Double::class
).associateBy { it.simpleName!! }

fun generateFormulas(
    rootSpec: RootComponentSpec,
    componentsSpecs: List<ComponentSpec>,
    concernedFields: List<FieldAccessSpec>,
    expressionsDependingOnRequiredOutput: Map<String, (FormulaArguments) -> Any>
): List<Formula> {
    val formulas = mutableListOf<Formula>()
    for ((requiredOutput, expression) in expressionsDependingOnRequiredOutput) {
        formulas.add(Formula(
            rootSpec = rootSpec,
            componentsSpecs = componentsSpecs,
            requiredFields = concernedFields - FieldAccessSpec(requiredOutput),
            outputSpec = FieldAccessSpec(requiredOutput),
            expression = expression,
        ))
    }
    return formulas
}
