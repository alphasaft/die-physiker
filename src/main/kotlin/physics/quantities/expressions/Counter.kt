package physics.quantities.expressions

import Args
import noop
import physics.quantities.Quantity
import physics.quantities.doubles.PReal

class Counter(val name: String) : Expression() {
    override val members: Collection<Expression> = emptyList()
    override val complexity: Int = 1

    init {
        assertSimplified()
    }

    override fun derive(variable: String): Expression {
        return Const(0)
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int>): Quantity<PReal> {
        return PReal((counters[name] ?: throw NoSuchElementException("Counter $name wasn't provided.")).toDouble())
    }

    override fun evaluate(arguments: Args<VariableValue<PReal>>, counters: Args<Int>): PReal {
        return PReal((counters[name] ?: throw NoSuchElementException("Counter $name wasn't provided.")).toDouble())
    }

    override fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression {
        return ::noop
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
}