package physics.quantities.expressions

import Args
import noop
import physics.quantities.Quantity
import physics.quantities.PReal

class Indexing(
    private val seriesName: String,
    val indexer: Indexer,
): Expression() {

    sealed class Indexer {
        protected fun noSuchCounter(name: String): Nothing = throw IllegalArgumentException("Counter $name wasn't provided.")
        abstract fun getIndex(counters: Args<Int>): Int
        abstract override fun toString(): String
        abstract override fun equals(other: Any?): Boolean
        abstract override fun hashCode(): Int

        class Static(val value: Int) : Indexer() {
            override fun getIndex(counters: Args<Int>): Int = value
            override fun toString(): String = value.toString()
            override fun equals(other: Any?): Boolean = other is Static && other.value == this.value
            override fun hashCode(): Int = value.hashCode()
        }
        
        class UseCounter(val counterName: String, val a: Int = 1, val b: Int = 0) : Indexer() {
            override fun getIndex(counters: Args<Int>): Int = a*(counters[counterName] ?: noSuchCounter(counterName))+b
            override fun equals(other: Any?): Boolean = other is UseCounter && other.counterName == counterName && a == other.a && b == other.b
            override fun hashCode(): Int = ((counterName.hashCode())*31+a)*31+b
            override fun toString(): String {
                val prefix = if (a == 1) "" else a.toString()
                val suffix = if (b == 0) "" else "+$b"
                return "$prefix$counterName$suffix"
            }
        }
    }

    override val members: Collection<Expression> = emptyList()
    override val complexity: Int = 1

    init {
        assertSimplified()
    }

    private fun indexingUses(counter: String): Boolean {
        return indexer is Indexer.UseCounter && indexer.counterName == counter
    }

    private fun withIndexer(indexer: Indexer): Indexing {
        return Indexing(seriesName, indexer)
    }

    fun increaseCounterBy(counter: String, n: Int): Indexing {
        if (!indexingUses(counter)) return this
        val indexer = this.indexer as Indexer.UseCounter
        return withIndexer(Indexer.UseCounter(indexer.counterName, b = indexer.b + indexer.a*n))
    }

    fun maximalCounterValueFor(counter: String, arguments: Args<VariableValue<*>>): Int {
        return when (indexer) {
            is Indexer.Static -> Int.MAX_VALUE
            is Indexer.UseCounter -> if (counter != indexer.counterName) Int.MAX_VALUE else {
                val series = arguments[seriesName] ?: throw NoSuchElementException("Variable $seriesName wasn't provided.")
                require(series is VariableValue.Array<*>) { "Expected a series, got a simple value." }
                return (series.size - indexer.b)/indexer.a
            }
        }
    }

    override fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression {
        return ::noop
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int>): Quantity<PReal> {
        val series = arguments[seriesName] ?: throw NoSuchElementException("Variable $seriesName wasn't provided.")
        val counter = indexer.getIndex(counters)
        require(series is VariableValue.Array) { "Expected a series, got a single value." }
        return series[counter]
    }

    override fun evaluate(arguments: Args<VariableValue<PReal>>, counters: Args<Int>): PReal {
        val series = arguments[seriesName] ?: throw NoSuchElementException("Variable $seriesName wasn't provided.")
        val counter = indexer.getIndex(counters)
        require(series is VariableValue.Array) { "Expected a series, got a single value." }
        return series[counter]
    }

    override fun simplifyImpl(): Expression {
        return this
    }

    override fun mayBeDiscontinuousImpl(): Boolean {
        return false
    }

    override fun toString(): String {
        return "$seriesName[$indexer]"
    }

    override fun equals(other: Any?): Boolean {
        return other is Indexing && other.seriesName == seriesName && other.indexer == indexer
    }

    override fun hashCode(): Int {
        var result = seriesName.hashCode()
        result *= result * 31 + indexer.hashCode()
        return result
    }

    override fun withMembers(members: List<Expression>): Expression {
        return this
    }

    override fun derive(variable: String): Expression {
        return Const(0)
    }
}
