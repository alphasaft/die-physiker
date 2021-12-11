package physics.computation.expressions

import physics.noop
import physics.values.NeutralValuesFactory
import physics.values.PhysicalDouble
import println


class Const(val value: PhysicalDouble) : Expression() {
    constructor(value: Double): this(NeutralValuesFactory.double(value))
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
            if ((value*i).isInt()) {
                val fractionalRepr = "${(value*i).toPhysicalInt()}/$i"
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

    override fun evaluate(args: Map<String, PhysicalDouble>): PhysicalDouble {
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
