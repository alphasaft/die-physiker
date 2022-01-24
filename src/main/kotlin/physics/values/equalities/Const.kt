package physics.values.equalities

import noop
import physics.quantities.Quantity
import physics.quantities.doubles.*


class Const(val value: PReal) : Expression() {
    constructor(value: Double): this(PReal(value))
    constructor(value: Int): this(value.toDouble())

    init {
        assertSimplified()
    }

    override val members: Collection<Expression> = emptyList()
    override val size: Int = 1

    override fun toString(): String {
        val defaultRepr = value.toString()
        if (value.isInt()) return defaultRepr

        for (i in 1..100) {
            val numerator = value * PReal(i.toDouble())
            if (numerator.isInt()) {
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

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        return ::noop
    }

    override fun evaluate(arguments: Map<String, Quantity<PReal>>): Quantity<PReal> {
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
    operator fun times(other: Const) = Const(value * other.value)
    operator fun div(other: Const) = Const(value / other.value)
}
