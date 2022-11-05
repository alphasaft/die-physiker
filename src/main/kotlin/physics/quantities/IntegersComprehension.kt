package physics.quantities


import Mapper
import Predicate
import alwaysTrue
import physics.quantities.units.PUnit
import kotlin.reflect.KClass


class IntegersComprehension(
    override val unit: PUnit,
    private val functions: Set<Pair<InDomain, PFunction>>
) : PDoubleOperand {

    constructor(
        unit: PUnit,
        vararg functions: Pair<InDomain, PFunction>
    ): this(unit, functions.toSet())

    constructor(
        integersUnit: PUnit,
        inDomain: InDomain = InDomain.N,
        function: PFunction,
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

    private class MultipliedByScalarPFunction private constructor(private val aExt: PDouble, private val f: PFunction, private val aInt: PDouble) : PFunction {
        constructor(aExt: PDouble, f: PFunction): this(aExt, f, aExt.withValue(0.0))

        override val outDomain: Quantity<PDouble> = aExt * f.outDomain
        override val reciprocal: PFunction = MultipliedByScalarPFunction(aInt.inv(), f.reciprocal, aExt.inv())
        override val derivative: PFunction = MultipliedByScalarPFunction(aExt * aInt, f.derivative, aInt)
        override fun invoke(x: PDouble): PDouble = aExt * f.invoke(x * aInt)
        override fun invoke(x: String): String = "$aExt${f("$aInt$x")}"
        override fun invokeExhaustively(x: Quantity<PDouble>): Quantity<PDouble> = aExt * f.invokeExhaustively(aInt * x)
    }

    private class AddedWithScalarPFunction private constructor(private val aExt: PDouble, private val f: PFunction, private val aInt: PDouble) : PFunction {
        constructor(aExt: PDouble, f: PFunction): this(aExt, f, aExt.withValue(0.0))

        override val outDomain: Quantity<PDouble> = aExt + f.outDomain
        override val reciprocal: PFunction = AddedWithScalarPFunction(-aInt, f.reciprocal, -aExt)
        override val derivative: PFunction = AddedWithScalarPFunction(PDouble(0.0), f.derivative, aInt)
        override fun invoke(x: PDouble): PDouble = aExt + f(x + aInt) 
        override fun invoke(x: String): String = "$aExt + ${f("$x+$aInt")}"
        override fun invokeExhaustively(x: Quantity<PDouble>): Quantity<PDouble> = aExt + f.invokeExhaustively(x + aInt)
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
            is PDoubleInterval -> this stdIntersect quantity
            else -> QuantityIntersection.assertReduced(this, quantity)
        }
    }

    private infix fun stdIntersect(interval: PDoubleInterval): Quantity<PDouble> {
        val default = QuantityIntersection.assertReduced(this, interval)
        val intervalPullbacks = functions.map { (inDomain, f) -> Triple(inDomain, f, f.reciprocal.invokeExhaustively(interval)) }
        var result: Quantity<PDouble> = AnyQuantity()

        for ((inDomain, f, pullback) in intervalPullbacks) {
            for (pullbackPart in pullback.simplify().let { if (it is QuantityUnion) it.items else listOf(it) }) {
                if (pullbackPart !is PDoubleInterval || pullbackPart.amplitude.isInfinite()) {
                    return default
                }

                val integersOnlyForPullbackPart = pullbackPart.integersOnly(unit).filter { it.toInt() in inDomain }
                result = result intersect QuantityUnion.assertReduced(*integersOnlyForPullbackPart.map { i -> f(i) }.toTypedArray())
            }
        }

        return result
    }

    private fun mapFunctions(mapper: Mapper<PFunction>): IntegersComprehension {
        return IntegersComprehension(unit, functions.map { (inDomain, f) -> inDomain to mapper(f) }.toSet())
    }

    override fun unaryMinus(): Quantity<PDouble> {
        return mapFunctions { f -> MultipliedByScalarPFunction(PDouble(-1.0), f) }
    }

    override fun plus(other: PDoubleOperand): Quantity<PDouble> {
        return when (other) {
            is PDouble -> this + other
            is PDoubleInterval -> AnyQuantity()
            is IntegersComprehension -> AnyQuantity()
            else -> AnyQuantity()
        }
    }

    private operator fun plus(other: PDouble): Quantity<PDouble> {
        return mapFunctions { f -> AddedWithScalarPFunction(other, f) }
    }

    override fun times(other: PDoubleOperand): Quantity<PDouble> {
        return when (other) {
            is PDouble -> this * other
            is PDoubleInterval -> AnyQuantity()
            is IntegersComprehension -> AnyQuantity()
            else -> AnyQuantity()
        }
    }

    private operator fun times(other: PDouble): Quantity<PDouble> {
        return mapFunctions { f -> MultipliedByScalarPFunction(other, f) }
    }

    override fun inv(): Quantity<PDouble> {
        return AnyQuantity()
    }

    override fun pow(other: PDoubleOperand): Quantity<PDouble> {
        return AnyQuantity()
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
