package physics.quantities

import physics.quantities.doubles.applyFunction


interface Function  {
    val outDomain: Quantity<PReal>

    val reciprocal: Function

    operator fun invoke(x: String): String
    operator fun invoke(x: PReal): PReal
    fun invokeExhaustively(x: Quantity<PReal>): Quantity<PReal>

    fun compose(f: Function): Function = defaultComposition(this, f)

    companion object Util {
        fun defaultComposition(f: Function, g: Function): Function {
            return object : Function {
                override val outDomain: Quantity<PReal> get() = g.outDomain.applyFunction(f)
                override val reciprocal: Function get() = g.reciprocal.compose(f.reciprocal)

                override fun invoke(x: String): String = f(g(x))
                override fun invoke(x: PReal): PReal = f(g(x))
                override fun invokeExhaustively(x: Quantity<PReal>): Quantity<PReal> = g.invokeExhaustively(x).applyFunction(f)
            }
        }
    }
}
