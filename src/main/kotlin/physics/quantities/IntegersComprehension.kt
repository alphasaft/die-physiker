package physics.quantities


import Predicate
import alwaysTrue
import physics.quantities.expressions.*
import physics.quantities.units.PUnit
import kotlin.reflect.KClass


class IntegersComprehension(
    private val unit: PUnit,
    private val functions: Set<Pair<InDomain, Function>>
) : PRealOperand {

    constructor(inDomain: InDomain): this(PUnit(), inDomain, IdentityFunction)

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

    object IdentityFunction : Function {
        override val outDomain: Quantity<PDouble> = AnyQuantity()
        override val reciprocal: Function = IdentityFunction
        override fun invoke(x: String): String = x
        override fun invoke(x: PDouble): PDouble = x
        override fun invokeExhaustively(x: Quantity<PDouble>): Quantity<PDouble> = x
    }

    override val type: KClass<PDouble> = PDouble::class

    override fun contains(value: PDouble): Boolean {
        for ((inDomain, f) in functions) if (f.reciprocal(value).convertInto(unit).let { !it.isInt() || it.toInt() !in inDomain }) return false
        return true
    }

    override fun simplify(): Quantity<PDouble> {
        return this
    }

    override fun simpleUnion(quantity: Quantity<PDouble>): Quantity<PDouble> {
        return when {
            this intersect quantity == this -> quantity
            this intersect quantity == quantity -> this
            else -> QuantityUnion.assertReduced(this, quantity)
        }
    }

    override fun simpleIntersect(quantity: Quantity<PDouble>): Quantity<PDouble> {
        return when (quantity) {
            is PRealInterval -> this stdIntersect quantity
            else -> QuantityIntersection.assertReduced(this, quantity)
        }
    }

    private infix fun stdIntersect(interval: PRealInterval): Quantity<PDouble> {
        val default = QuantityIntersection.assertReduced(this, interval)
        val intervalPullbacks = functions.map { (inDomain, f) -> Triple(inDomain, f, f.reciprocal.invokeExhaustively(interval)) }
        var result: Quantity<PDouble> = AnyQuantity()

        for ((inDomain, f, pullback) in intervalPullbacks) {
            for (pullbackPart in pullback.simplify().let { if (it is QuantityUnion) it.items else listOf(it) }) {
                if (pullbackPart !is PRealInterval || pullbackPart.amplitude.isInfinite()) {
                    return default
                }

                val integersOnlyForPullbackPart = pullbackPart.integersOnly(unit).filter { it.toInt() in inDomain }
                result = result intersect QuantityUnion.assertReduced(*integersOnlyForPullbackPart.map { i -> f(i) }.toTypedArray())
            }
        }

        return result
    }

    private fun composeFunctionsBy(f: Function): IntegersComprehension {
        return IntegersComprehension(unit, functions.map { (inDomain, g) -> inDomain to f.compose(g) }.toSet())
    }

    override fun unaryMinus(): Quantity<PDouble> {
        return composeFunctionsBy((-v("x")).toFunction("x"))
    }

    override fun plus(other: PRealOperand): Quantity<PDouble> {
        return when (other) {
            is PDouble -> this + other
            is PRealInterval -> AnyQuantity()
            is IntegersComprehension -> AnyQuantity()
            else -> AnyQuantity()
        }
    }

    private operator fun plus(other: PDouble): Quantity<PDouble> {
        return composeFunctionsBy((v("x") + c(other)).toFunction("x"))
    }

    override fun times(other: PRealOperand): Quantity<PDouble> {
        return when (other) {
            is PDouble -> this * other
            is PRealInterval -> AnyQuantity()
            is IntegersComprehension -> AnyQuantity()
            else -> AnyQuantity()
        }
    }

    private operator fun times(other: PDouble): Quantity<PDouble> {
        return composeFunctionsBy((v("x") * c(other)).toFunction("x"))
    }

    override fun div(other: PRealOperand): Quantity<PDouble> {
        return when (other) {
            is PDouble -> this / other
            is PRealInterval -> AnyQuantity()
            is IntegersComprehension -> AnyQuantity()
            else -> AnyQuantity()
        }
    }

    private operator fun div(other: PDouble): Quantity<PDouble> {
        return composeFunctionsBy((v("x") / c(other)).toFunction("x"))
    }

    override fun pow(other: PRealOperand): Quantity<PDouble> {
        return when (other) {
            is PDouble -> this.pow(other)
            is PRealInterval -> AnyQuantity()
            is IntegersComprehension -> AnyQuantity()
            else -> AnyQuantity()
        }
    }

    private fun pow(other: PDouble): Quantity<PDouble> {
        return composeFunctionsBy((v("x").pow(c(other)).toFunction("x")))
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
