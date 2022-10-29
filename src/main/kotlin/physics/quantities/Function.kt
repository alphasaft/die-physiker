package physics.quantities


interface Function  {
    val outDomain: Quantity<PDouble>

    val reciprocal: Function

    operator fun invoke(x: String): String
    operator fun invoke(x: PDouble): PDouble
    fun invokeExhaustively(x: Quantity<PDouble>): Quantity<PDouble>

    fun compose(f: Function): Function = defaultComposition(this, f)

    companion object Util {
        fun defaultComposition(f: Function, g: Function): Function {
            return object : Function {
                override val outDomain: Quantity<PDouble> get() = g.outDomain.applyFunction(f)
                override val reciprocal: Function get() = g.reciprocal.compose(f.reciprocal)

                override fun invoke(x: String): String = f(g(x))
                override fun invoke(x: PDouble): PDouble = f(g(x))
                override fun invokeExhaustively(x: Quantity<PDouble>): Quantity<PDouble> = g.invokeExhaustively(x).applyFunction(f)
            }
        }
    }
}
