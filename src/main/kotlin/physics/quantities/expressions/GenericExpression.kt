package physics.quantities.expressions

import Args
import physics.quantities.Quantity
import physics.quantities.PDouble
import kotlin.reflect.KClass
import kotlin.reflect.KFunction1


abstract class GenericExpression(
    protected val term: Expression,
    protected val counterName: String,
    protected val start: Bound = Bound.Static(1),
    protected val end: Bound,
) : Expression() {

    protected val counter = Counter(counterName)
    override val members: Collection<Expression> = listOf(term)
    protected abstract val associatedStandardExpressionCtr: KFunction1<List<Expression>, Expression>

    protected abstract val reducer1: (PDouble, PDouble) -> PDouble
    protected abstract val reducer2: (Quantity<PDouble>, Quantity<PDouble>) -> Quantity<PDouble>

    sealed class Bound {
        abstract fun evaluate(arguments: Args<VariableValue<*>>): Int

        class Static(val n: Int) : Bound() {
            override fun toString(): String = n.toString()
            override fun evaluate(arguments: Args<VariableValue<*>>): Int = n
        }

        class SeriesSize(
            private val seriesName: String,
            private val a: Int = 1,
            private val b: Int = 0,
        ) : Bound() {
            override fun toString(): String {
                val prefix = if (a == 1) "" else a.toString()
                val suffix = if (b == 0) "" else if (b < 0) "-${-b}" else "+$b"
                return "$prefix$seriesName$suffix"
            }

            override fun evaluate(arguments: Args<VariableValue<*>>): Int {
                val series = arguments[seriesName] ?: throw NoSuchElementException("Variable $seriesName wasn't provided")
                require(series is VariableValue.Array) { "Expected a series, got a single value" }
                return a*series.size+b
            }
        }
    }

    override fun evaluate(arguments: Args<VariableValue<PDouble>>, counters: Args<Int>): PDouble {
        val collected = mutableListOf<PDouble>()

        for (i in start.evaluate(arguments)..end.evaluate(arguments)) {
            collected.add(term.evaluate(arguments, counters + Pair(counterName, i)))
        }

        return collected.reduce(reducer1)
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int>): Quantity<PDouble> {
        val collected = mutableListOf<Quantity<PDouble>>()

        for (i in start.evaluate(arguments)..end.evaluate(arguments)) {
            collected.add(term.evaluateExhaustively(arguments, counters + Pair(counterName, i)))
        }

        return collected.reduce(reducer2)
    }

    override fun toString(): String {
        val functionName = (associatedStandardExpressionCtr.returnType.classifier as KClass<*>).simpleName
        return "$functionName($counterName, $start, $end) { $term } "
    }

    override fun equals(other: Any?): Boolean {
        return other is GenericExpression && this::class == other::class && term == other.term
    }

    override fun hashCode(): Int {
        return this::class.hashCode() * 7 + term.hashCode() * 13
    }
}