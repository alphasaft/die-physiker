package physics.quantities.doubles

import physics.quantities.Quantity
import kotlin.reflect.KFunction1

class MathFunction(
    val name: String = "<anonymous math function>",
    private val f: (Double) -> Double,
    val inDomain: Quantity<PReal>,
    val outDomain: Quantity<PReal>,
) {
    constructor(
        f: KFunction1<Double, Double>,
        inDomain: Quantity<PReal>,
        outDomain: Quantity<PReal>
    ): this(f.name, f, inDomain, outDomain)

    operator fun invoke(x: Double) = f(x)

    override fun toString(): String {
        return "name : $inDomain |--> $outDomain"
    }
}
