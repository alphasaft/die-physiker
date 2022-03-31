package physics.quantities.expressions

import Args
import noop
import physics.quantities.Quantity
import physics.quantities.PReal

class Var(val name: String) : Expression() {
    override val members: Collection<Expression> = emptyList()
    override val complexity: Int = 1

    init {
        assertSimplified()
    }

    override fun mayBeDiscontinuousImpl(): Boolean {
        return false
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int>): Quantity<PReal> {
        val value = arguments[name] ?: throw NoSuchElementException("Variable $name wasn't provided.")
        require(value is VariableValue.Single) { "Expected a single value, got a series." }
        return value.content
    }

    override fun evaluate(arguments: Args<VariableValue<PReal>>, counters: Args<Int>): PReal {
        val value = arguments[name] ?: throw NoSuchElementException("Variable $name wasn't provided.")
        require(value is VariableValue.Single) { "Expected a single value, got a series." }
        return value.content
    }

    override fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression {
        return ::noop
    }

    override fun toString(): String {
        return name
    }

    override fun simplifyImpl(): Expression {
        return this
    }

    override fun equals(other: Any?): Boolean {
        return other is Var && other.name == name
    }

    override fun hashCode(): Int {
        return (Var::class.hashCode() * 7 + name.hashCode() * 13)
    }

    override fun withMembers(members: List<Expression>): Expression {
        return this
    }

    override fun derive(variable: String): Expression {
        return if (variable == name) Const(1) else Const(0)
    }

}
