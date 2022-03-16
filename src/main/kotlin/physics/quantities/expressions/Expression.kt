package physics.quantities.expressions

import Args
import Predicate
import alwaysTrue
import noop
import physics.quantities.AnyQuantity
import physics.quantities.Quantity
import physics.quantities.doubles.PReal
import safe
import kotlin.reflect.KClass


abstract class Expression {
    abstract val members: Collection<Expression>
    open val complexity get() = members.size

    open val outDomain: Quantity<PReal> get() = evaluateExhaustively(allVariables().associateWith<String, VariableValue<Quantity<PReal>>> { VariableValue.Single(AnyQuantity()) })

    private var simplified: Expression? = null
        set(value) {
            requireNotNull(value)
            field = value
            value.assertSimplified()
        }


    protected fun assertSimplified() {
        if (simplified != this) simplified = this
    }

    fun simplify(): Expression {
        return if (simplified != null) simplified!!
        else this.simplifyImpl().also { simplified = it }
    }

    protected abstract fun simplifyImpl(): Expression


    fun mayBeDiscontinuous(): Boolean {
        return simplify().mayBeDiscontinuousImpl()
    }

    protected abstract fun mayBeDiscontinuousImpl(): Boolean


    fun asFunction(parameter: String): ExpressionAsFunction {
        return ExpressionAsFunction(this, parameter)
    }

    operator fun contains(member: Expression): Boolean {
        return member == this || member in allMembers()
    }

    fun allMembers(): List<Expression> {
        return members + members.map { it.allMembers() }.flatten()
    }

    fun allVariables(): List<String> {
        return (allMembers() + this).filterIsInstance<Var>().map { it.name }
    }

    fun allCounters(): List<String> {
        return (allMembers() + this).filterIsInstance<Counter>().map { it.name }
    }



    abstract fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression

    fun getVariableIsoler(variableName: String): (Expression) -> Expression {
        val variable = Var(variableName)
        if (members.count { variable == it || variable in it } > 1) throw UnsolvableVariable()
        if (variable in members) return getDirectMemberIsoler(member = members.single { variable == it })
        if (members.count { variable in it } == 0) return ::noop

        val concernedMember = members.single { variable in it }
        return { concernedMember.getVariableIsoler(variableName).invoke(getDirectMemberIsoler(concernedMember).invoke(it)) }
    }

    fun substitute(old: Expression, new: Expression): Expression = substitute<Expression>({ it == old }, { new })
    inline fun <reified T : Expression> substitute(noinline filter: Predicate<T> = ::alwaysTrue, noinline mapper: (T) -> Expression): Expression = substitute(T::class, filter, mapper)
    fun <T : Expression> substitute(eType: KClass<T>, filter: Predicate<T>, mapper: (T) -> Expression): Expression {
        if (eType.isInstance(this) && filter(safe(this))) return mapper(safe(this))
        return withMembers(members.map { it.substitute(eType, filter, mapper) })
    }

    fun substituteAll(substitutions: Map<Expression, Expression>): Expression {
        var result = this
        for ((old, new) in substitutions) {
            result = result.substitute(old, new)
        }
        return result
    }


    abstract fun withMembers(members: List<Expression>): Expression

    abstract fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int> = emptyMap()): Quantity<PReal>

    abstract fun evaluate(arguments: Args<VariableValue<PReal>>, counters: Args<Int> = emptyMap()): PReal

    abstract fun derive(variable: String): Expression

    override fun equals(other: Any?): Boolean {
        if (members.isEmpty()) throw IllegalStateException("Expression with no member should override equals().")
        if (other !is Expression) return false
        if (other.members.isEmpty()) return other == this
        return this::class == other::class && this.members == other.members
    }

    override fun hashCode(): Int {
        var result = this::class.hashCode()
        result = 31 * result + members.hashCode()
        return result
    }
}
