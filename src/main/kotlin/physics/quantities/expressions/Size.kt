package physics.quantities.expressions

import Args
import noop
import physics.quantities.Quantity
import physics.quantities.PReal

class Size(private val seriesName: String) : Expression() {
    override val members: Collection<Expression> = emptyList()

    init {
        assertSimplified()
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int>): Quantity<PReal> {
        val series = arguments[seriesName] ?: throw NoSuchElementException("Variable $seriesName wasn't provided.")
        require(series is VariableValue.Array) { "Expected a series, got a single value." }
        return PReal(series.size)
    }

    override fun evaluate(arguments: Args<VariableValue<PReal>>, counters: Args<Int>): PReal {
        val series = arguments[seriesName] ?: throw NoSuchElementException("Variable $seriesName wasn't provided.")
        require(series is VariableValue.Array) { "Expected a series, got a single value." }
        return PReal(series.size)
    }

    override fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression {
        return ::noop
    }

    override fun simplifyImpl(): Expression {
        return this
    }

    override fun withMembers(members: List<Expression>): Expression {
        return this
    }

    override fun derive(variable: String): Expression {
        return Const(0)
    }

    override fun mayBeDiscontinuousImpl(): Boolean {
        return false
    }

    override fun toString(): String {
        return "size($seriesName)"
    }

    override fun equals(other: Any?): Boolean {
        return other is Size && other.seriesName == seriesName
    }

    override fun hashCode(): Int {
        return seriesName.hashCode()
    }
}