package physics.values.equalities

import kotlin.math.E


class Exp(x: Expression) : Pow(Const(E), x) {
    override fun toString(): String {
        return "exp($exponent)"
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        return if (member == exponent) {{ Ln(it) }}
        else super.isolateDirectMember(member)
    }
}