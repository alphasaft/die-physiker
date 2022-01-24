package physics.values.equalities

import physics.quantities.AnyQuantity
import physics.quantities.Quantity
import physics.quantities.asPValue
import physics.quantities.doubles.MathFunction
import physics.quantities.doubles.PReal
import physics.quantities.doubles.PRealInterval
import physics.quantities.doubles.applyContinuousFunction
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.tan
import kotlin.math.*
import kotlin.reflect.KFunction1


class MathFunctionCall(
    private val functionName: String,
    private val argument: Expression,
) : Expression() {
    private companion object {

        private val functionsAndReciprocals = listOf(
            MathFunction(::sin, AnyQuantity(), PRealInterval.Builtin.fromMinus1To1) to MathFunction(::asin, PRealInterval.Builtin.fromMinus1To1, PRealInterval.Builtin.fromMinusHalfPiToHalfPi),
            MathFunction(::cos, AnyQuantity(), PRealInterval.Builtin.fromMinus1To1) to MathFunction(::acos, PRealInterval.Builtin.fromMinus1To1, PRealInterval.Builtin.fromMinusHalfPiToHalfPi),
            functionFromRToR(::tan) to functionFromRToR(::atan),
            functionFromRToR(::cosh) to functionFromRToR(::acosh),
            functionFromRToR(::sinh) to functionFromRToR(::asinh),
        ).let { it + it.map { (f, r) -> Pair(r, f) } }

        private fun functionFromRToR(f: KFunction1<Double, Double>) = MathFunction(f, AnyQuantity(), AnyQuantity())

        val reciprocals: Map<String, String> = mapOf(*functionsAndReciprocals.map { (f, r) -> Pair(f.name, r.name) }.toTypedArray())
        val functions: Map<String, MathFunction> = functionsAndReciprocals.map { (f, _) -> f }.associateBy{ it.name }
    }

    val function = functions[functionName]  ?: throw NoSuchElementException("Mathematical function $functionName doesn't exist. ")

    override val members: Collection<Expression> = listOf(argument)

    override fun evaluate(arguments: Map<String, Quantity<PReal>>): Quantity<PReal> {
        return argument.evaluate(arguments).applyContinuousFunction(function)
    }

    override fun simplifyImpl(): Expression {
        return when {
            argument is MathFunctionCall && argument.functionName === reciprocals.getValue(functionName) -> argument
            argument is Const -> {
                val result = argument.value.applyContinuousFunction(function)
                if (result is PReal) return Const(result)
                return this
            }
            else ->this
        }
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        return { MathFunctionCall(reciprocals.getValue(functionName), it) }
    }

    override fun toString(): String {
        return "$functionName($argument)"
    }

    override fun withMembers(members: List<Expression>): Expression {
        val argument = members.single()
        return MathFunctionCall(functionName, argument)
    }
}
