package physics.quantities.expressions

import Args
import noop
import physics.quantities.PDouble
import physics.quantities.Quantity


class Const(val value: PDouble) : Expression() {
    constructor(value: Double): this(PDouble(value))
    constructor(value: Int): this(value.toDouble())

    init {
        assertSimplified()
    }

    override val members: Collection<Expression> = emptyList()
    override val complexity: Int = 1

    override fun toString(): String {
        val defaultRepr = value.toString()
        if (value.isInt()) return defaultRepr
        if (!value.unit.isNeutral()) return defaultRepr

        for (i in listOf(2, 3, 4, 5)) {
            val numerator = value * PDouble(i.toDouble())
            if (numerator.isInt() && numerator.withoutUnit() <= PDouble(10)) {
                val fractionalRepr = "${numerator.toPInt()}/$i"
                return if (fractionalRepr.length <= defaultRepr.length) fractionalRepr
                else defaultRepr
            }
        }

        return defaultRepr
    }

    override fun equals(other: Any?): Boolean {
        return other is Const && other.value == this.value
    }

    override fun hashCode(): Int {
        return Const::class.hashCode() * 7 + value.hashCode() * 13
    }

    override fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression {
        return ::noop
    }

    override fun evaluate(arguments: Args<VariableValue<PDouble>>, counters: Args<Int>): PDouble {
        return value
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Map<String, Int>): Quantity<PDouble> {
        return value
    }

    override fun simplifyImpl(): Expression {
        return this
    }

    override fun mayBeDiscontinuousImpl(): Boolean {
        return false
    }

    override fun withMembers(members: List<Expression>): Expression {
        return this
    }

    override fun differentiate(variable: String): Expression {
        return Const(0)
    }

    operator fun plus(other: Const) = Const(value + other.value)
    operator fun minus(other: Const) = Const(value - other.value)
    operator fun times(other: Const) = Const(value * other.value)
    operator fun div(other: Const) = Const(value / other.value)
}
