package physics.quantities.doubles


import Predicate
import alwaysTrue
import physics.quantities.*
import physics.quantities.expressions.*
import physics.quantities.units.PUnit
import println
import kotlin.reflect.KClass


class IntegersComprehension(
    private val unit: PUnit,
    private val functions: Set<Pair<InDomain, Function>>
) : PRealOperand {

    constructor(
        unit: PUnit,
        vararg functions: Pair<InDomain, Function>
    ): this(unit, functions.toSet())

    constructor(
        integersUnit: PUnit,
        inDomain: InDomain = InDomain.N,
        function: Function,
    ): this(integersUnit, inDomain to function)

    enum class InDomain(private val predicate: Predicate<Int>, private val asString: String) {
        N({ it >= 0 }, "N"),
        Z(::alwaysTrue, "Z"),
        NWithout0({ it > 0 }, "N*"),
        ZWithout0({ it != 0}, "Z*"),
        ;

        operator fun contains(n: Int) = predicate(n)
        override fun toString(): String = asString
    }

    override val type: KClass<PReal> = PReal::class

    override fun contains(value: PReal): Boolean {
        for ((inDomain, f) in functions) if (f.reciprocal(value).convertInto(unit).let { !it.isInt() || it.toInt() !in inDomain }) return false
        return true
    }

    override fun simplify(): Quantity<PReal> {
        return this
    }

    override fun stdUnion(quantity: Quantity<PReal>): Quantity<PReal> {
        return when {
            this intersect quantity == this -> quantity
            this intersect quantity == quantity -> this
            else -> QuantityUnion.assertReduced(this, quantity)
        }
    }

    override fun stdIntersect(quantity: Quantity<PReal>): Quantity<PReal> {
        return when (quantity) {
            is PRealInterval -> this stdIntersect quantity
            else -> QuantityIntersection.assertReduced(this, quantity)
        }
    }

    private infix fun stdIntersect(interval: PRealInterval): Quantity<PReal> {
        val default = QuantityIntersection.assertReduced(this, interval)
        val intervalPullbacks = functions.map { (inDomain, f) -> Triple(inDomain, f, f.reciprocal.invokeExhaustively(interval)) }
        var result: Quantity<PReal> = AnyQuantity()

        for ((inDomain, f, pullback) in intervalPullbacks) {
            for (pullbackPart in pullback.simplify().let { if (it is QuantityUnion) it.items else listOf(it) }) {
                if (pullbackPart !is PRealInterval || pullbackPart.hasInfiniteAmplitude()) {
                    return default
                }

                val integersOnlyForPullbackPart = pullbackPart.integersOnly(unit).filter { it.toInt() in inDomain }
                result = result intersect QuantityUnion.assertReduced(*integersOnlyForPullbackPart.map { i -> f(i) }.toTypedArray())
                println(result)
            }
        }

        return result
    }

    private fun composeFunctionsBy(f: Function): IntegersComprehension {
        return IntegersComprehension(unit, functions.map { (inDomain, g) -> inDomain to f.compose(g) }.toSet())
    }

    override fun unaryMinus(): Quantity<PReal> {
        return composeFunctionsBy((-v("x")).asFunction("x"))
    }

    override fun plus(other: PRealOperand): Quantity<PReal> {
        return when (other) {
            is PReal -> this + other
            is PRealInterval -> AnyQuantity()
            is IntegersComprehension -> AnyQuantity()
            else -> AnyQuantity()
        }
    }

    private operator fun plus(other: PReal): Quantity<PReal> {
        return composeFunctionsBy((v("x") + c(other)).asFunction("x"))
    }

    override fun times(other: PRealOperand): Quantity<PReal> {
        return when (other) {
            is PReal -> this * other
            is PRealInterval -> AnyQuantity()
            is IntegersComprehension -> AnyQuantity()
            else -> AnyQuantity()
        }
    }

    private operator fun times(other: PReal): Quantity<PReal> {
        return composeFunctionsBy((v("x") * c(other)).asFunction("x"))
    }

    override fun div(other: PRealOperand): Quantity<PReal> {
        return when (other) {
            is PReal -> this / other
            is PRealInterval -> AnyQuantity()
            is IntegersComprehension -> AnyQuantity()
            else -> AnyQuantity()
        }
    }

    private operator fun div(other: PReal): Quantity<PReal> {
        return composeFunctionsBy((v("x") / c(other)).asFunction("x"))
    }

    override fun pow(other: PRealOperand): Quantity<PReal> {
        return when (other) {
            is PReal -> this.pow(other)
            is PRealInterval -> AnyQuantity()
            is IntegersComprehension -> AnyQuantity()
            else -> AnyQuantity()
        }
    }

    private fun pow(other: PReal): Quantity<PReal> {
        return composeFunctionsBy((v("x").pow(c(other)).asFunction("x")))
    }

    override fun toString(): String {
        if (functions.size == 1) {
            val (inDomain, f) = functions.single()
            return "{ ${f("n")} | n <- $inDomain }"
        }

        var nIndex = 1
        val conditions = functions.joinToString(" && ") { (inDomain, f) -> "x = ${f("n$nIndex")}, n${nIndex++} <- $inDomain" }

        return "{ x | $conditions }"
    }

    override fun equals(other: Any?): Boolean {
        return other is IntegersComprehension && other.functions == functions
    }

    override fun hashCode(): Int {
        return functions.hashCode()
    }
}
