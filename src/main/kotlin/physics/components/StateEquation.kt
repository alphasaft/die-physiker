package physics.components

import ifMissing
import physics.quantities.expressions.Equation
import physics.rules.Formula


class StateEquation private constructor(private val equation: Equation?) {
    class Template(vararg variables: String) {
        private val variables = variables.toSet()

        fun create(equation: Equation? = null): StateEquation {
            if (equation != null) {
                val equationVariables = equation.allVariables()
                equationVariables.ifMissing(expected = variables) { "Expected variable $it." }
                variables.ifMissing(expected = equationVariables) { "Variable $it was provided but not expected." }
            }
            return StateEquation(equation)
        }
    }

    fun isKnown(): Boolean {
        return equation != null
    }

    fun toFormula(): Formula {
        return Formula(equation ?: throw IllegalArgumentException("No equation was provided, thus no Formula can't be made out of it."))
    }

    override fun toString(): String {
        return equation.toString()
    }
}