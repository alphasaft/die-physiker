package physics.computation.formulas.expressions

import kotlin.math.E

class Ln(x: Expression) : Log(x, base = Const(E)) {
    override fun toString(): String {
        return "ln($x)"
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        return if (member == x) {{ Exp(it) }}
        else super.isolateDirectMember(member)
    }
}