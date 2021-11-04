package physics.computation.formulas.expressions

import Mapper
import physics.noop
import physics.values.PhysicalDouble


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
        if (simplified == null)
            simplified = this
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Expression) return false
        return this::class == other::class && this.members == other.members
    }

    override fun hashCode(): Int {
        return this::class.hashCode() * 7 + members.hashCode() * 13
    }

    abstract override fun toString(): String

    operator fun contains(member: Expression): Boolean {
        return member in members || members.any { member in it }
    }

    abstract fun evaluate(args: Map<String, PhysicalDouble>): PhysicalDouble

    protected abstract fun isolateDirectMember(member: Expression): (Expression) -> Expression
    fun isolateVariable(variableName: String): (Expression) -> Expression {
        val variable = Var(variableName)
        if (members.count { variable == it || variable in it } > 1) throw UnsolvableVariable()
        if (variable in members) return isolateDirectMember(member = members.single { variable == it })
        if (members.count { variable in it } == 0) return ::noop

        val concernedMember = members.single { variable in it }
        return { concernedMember.isolateVariable(variableName).invoke(isolateDirectMember(concernedMember).invoke(it)) }
    }

    fun simplify(): Expression {
        simplified?.let { return it }
        return simplifyImpl().also { simplified = it }
    }

    protected abstract fun simplifyImpl(): Expression

    protected inline fun <reified T : Expression> Iterable<Expression>.filterIsInstanceAndReplace(replacement: Mapper<List<T>>): List<Expression> {
        val unchanged = this.filterNot { it is T }
        val removed = this.filterIsInstance<T>()
        return unchanged + if (removed.isNotEmpty()) replacement(removed) else emptyList()
    }

    protected fun <T> Iterable<T>.filterOut(excluded: Iterable<T>): List<T> {
        return filter { it !in excluded }
    }

    fun substitute(variable: String, expression: Expression): Expression {
        val modifiedMembers = members.map { if (it == Var(variable)) expression else it }
        return withMembers(modifiedMembers)
    }

    abstract fun withMembers(members: List<Expression>): Expression
}

