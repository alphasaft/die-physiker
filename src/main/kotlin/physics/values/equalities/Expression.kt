package physics.values.equalities

import Mapper
import filterOut
import noop
import physics.quantities.Quantity
import physics.quantities.doubles.PReal


sealed class Expression {
    abstract val members: Collection<Expression>
    open val size get() = members.size

    private var simplified: Expression? = null
        set(value) {
            requireNotNull(value)
            field = value
            value.assertSimplified()
        }

    protected fun assertSimplified() {
        if (simplified == null) simplified = this
    }

    fun simplify(): Expression {
        simplified?.let { return it }
        return simplifyImpl().also { simplified = it }
    }

    protected abstract fun simplifyImpl(): Expression

    abstract override fun toString(): String

    override fun equals(other: Any?): Boolean {
        return other is Expression && this::class == other::class && this.members == other.members
    }

    override fun hashCode(): Int {
        return this::class.hashCode() * 7 + members.hashCode() * 13
    }

    operator fun contains(member: Expression): Boolean {
        return member in members || members.any { member in it }
    }

    private fun allMembers(): List<Expression> {
        return members + members.map { it.allMembers() }.flatten()
    }

    fun allVariables(): List<String> {
        return (allMembers() + this).filterIsInstance<Var>().map { it.name }
    }

    protected inline fun <reified T : Expression> Iterable<Expression>.filterIsInstanceAndReplace(replacement: Mapper<List<T>>): List<Expression> {
        val replaced = this.filterIsInstance<T>()
        val unchanged = this.filterOut(replaced)
        return unchanged + if (replaced.isNotEmpty()) replacement(replaced) else emptyList()
    }

    protected abstract fun isolateDirectMember(member: Expression): (Expression) -> Expression

    fun isolateVariable(variableName: String): (Expression) -> Expression {
        val variable = Var(variableName)
        if (members.count { variable == it || variable in it } > 1) throw UnsolvableVariable()
        if (variable in members) return isolateDirectMember(member = members.single { variable == it })
        if (members.count { variable in it } == 0) return ::noop

        val concernedMember = members.single { variable in it }
        return { concernedMember.isolateVariable(variableName).invoke(isolateDirectMember(concernedMember).invoke(it)) }
    }

    fun substitute(old: Expression, new: Expression): Expression {
        if (this == old) return new
        val modifiedMembers = members.map { it.substitute(old, new) }
        return withMembers(modifiedMembers)
    }

    open fun transformBy(allVariablesMappings: Map<String, String>): Expression {
        return withMembers(members.map { it.transformBy(allVariablesMappings) })
    }

    abstract fun withMembers(members: List<Expression>): Expression

    abstract fun evaluate(arguments: Map<String, Quantity<PReal>>): Quantity<PReal>

}
