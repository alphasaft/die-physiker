package physics.formulas.expressions

import physics.formulas.FormulaArguments
import physics.noop
import physics.values.PhysicalDouble

class Const(val value: PhysicalDouble) : Expression() {
    constructor(value: Double): this(PhysicalDouble(value))
    constructor(value: Int): this(value.toDouble())

    init {
        assertSimplified()
    }

    override val members: Collection<Expression> = emptyList()
    override val size: Int = 1

    override fun toString(): String {
        return value.toString()
    }

    override fun equals(other: Any?): Boolean {
        return other is Const && other.value == this.value
    }

    override fun hashCode(): Int {
        return Const::class.hashCode() * 7 + value.hashCode() * 13
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        return ::noop
    }

    override fun evaluate(args: FormulaArguments): PhysicalDouble {
        return value
    }

    override fun simplifyImpl(): Expression {
        return this
    }

    override fun withMembers(members: List<Expression>): Expression {
        return this
    }

    operator fun plus(other: Const) = Const(value + other.value)
    operator fun minus(other: Const) = Const(value - other.value)
    operator fun times(other: Const): Const = Const(value * other.value)
    operator fun div(other: Const) = Const(value / other.value)
}
