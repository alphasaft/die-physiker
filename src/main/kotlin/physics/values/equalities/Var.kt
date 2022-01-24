package physics.values.equalities

import noop
import physics.quantities.Quantity
import physics.quantities.doubles.PReal

class Var(val name: String) : Expression() {
    override val members: Collection<Expression> = emptyList()
    override val size: Int = 1

    init {
        assertSimplified()
    }

    override fun evaluate(arguments: Map<String, Quantity<PReal>>): Quantity<PReal> {
        return arguments[name] ?: throw NoSuchElementException("Variable $name wasn't provided.")
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
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

    override fun transformBy(allVariablesMappings: Map<String, String>): Expression {
        if (name !in allVariablesMappings) throw NoSuchElementException("Can't find variable $name in provided variables mappings.")
        return Var(allVariablesMappings.getValue(name))
    }
}
