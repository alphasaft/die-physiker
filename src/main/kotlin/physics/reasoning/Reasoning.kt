package physics.reasoning

class Reasoning(
    private val goal: Goal
) : Step {
    private val steps = mutableListOf<Step>()

    var result: Result? = null
        set(value) {
            require(value == null || goal.isSolvedBy(value))  { "Result $result doesn't answer to the initial goal $goal." }
            field = value
        }

    fun addStep(step: Step) {
        steps.add(step)
    }

    operator fun iterator() =
        steps.iterator()

    override fun toString(): String {
        return """
            But : $goal
            Étapes : ${steps.joinToString("\n" + " ".repeat(21))}
            Résultat : $result
        """.trimIndent()
    }
}
