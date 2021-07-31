package physics.formulas

import physics.formulas.expressions.Div
import physics.formulas.expressions.Expression
import physics.formulas.expressions.Var
import physics.values.PhysicalDouble


class Equality(
    val variable: String,
    val expression: Expression,
) {
    fun isolateVariable(variable: String): Equality {
        return if (variable == this.variable) this
        else Equality(variable, expression.isolateVariable(variable).invoke(Var(this.variable)))
    }

    override fun equals(other: Any?): Boolean {
        return other is Equality && variable == other.variable && expression == other.expression
    }

    override fun toString(): String {
        return if (expression is Div) variable + " = " + expression.toStringAsFraction(offset = variable.length + 3)
        else toFlatString()
    }

    override fun hashCode(): Int {
        return variable.hashCode() * 7 + expression.hashCode() * 13
    }

    fun compute(arguments: FormulaArguments): PhysicalDouble {
        return expression.evaluate(arguments)
    }

    fun composeWith(equality: Equality): Equality {
        return Equality(variable, expression.substitute(equality.variable, equality.expression))
    }

    fun toFlatString(): String {
        return "$variable = $expression"
    }
}

infix fun String.equal(other: Expression) = Equality(this, other)
