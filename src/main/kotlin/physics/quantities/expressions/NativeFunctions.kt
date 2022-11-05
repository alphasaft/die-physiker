package physics.quantities.expressions

import Args
import physics.quantities.*
import physics.quantities.PFunction
import kotlin.math.*
import kotlin.reflect.full.primaryConstructor


abstract class NativeFunction(val argument: Expression) : Expression() {
    final override val members: Collection<Expression> = listOf(argument)

    abstract val name: String
    abstract override val outDomain: Quantity<PDouble>
    abstract val isFunctionContinuous: Boolean
    abstract val functionDerivative: PFunctionFromExpression
    abstract val reciprocal: PFunctionFromExpression

    private val associatedFunction = object : PFunction {
        override val outDomain: Quantity<PDouble> get() = this@NativeFunction.outDomain
        override val reciprocal: PFunction get() = this@NativeFunction.reciprocal
        override val derivative: PFunction get() = this@NativeFunction.functionDerivative
        override fun invoke(x: String): String = "$name($x)"
        override fun invoke(x: PDouble): PDouble = this@NativeFunction(x)
        override fun invokeExhaustively(x: Quantity<PDouble>): Quantity<PDouble> = this@NativeFunction(x)
    }

    protected abstract operator fun invoke(x: PDouble): PDouble
    protected abstract operator fun invoke(x: PDoubleInterval): Quantity<PDouble>

    operator fun invoke(x: Quantity<PDouble>): Quantity<PDouble> {
        return when (x) {
            is PDouble -> this(x)
            is PDoubleInterval -> this(x)
            else -> AnyQuantity()
        }
    }

    final override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int>): Quantity<PDouble> {
        return argument.evaluateExhaustively(arguments, counters).applyFunction(associatedFunction)
    }

    final override fun evaluate(arguments: Args<VariableValue<PDouble>>, counters: Args<Int>): PDouble {
        return this(argument.evaluate(arguments, counters)).asPValue<PDouble>()
    }

    final override fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression {
        return { reciprocal(it) }
    }

    override fun simplifyImpl(): Expression {
        val simplifiedArgument = argument.simplify()

        if (simplifiedArgument is Const) {
            return Const(this(simplifiedArgument.value))
        }

        val simplified = if (simplifiedArgument is NativeFunction && simplifiedArgument::class == reciprocal(Var("x"))::class) simplifiedArgument.argument else this
        return if (simplified is NativeFunction) simplified.rewriteToSimplifyArgument() else simplified
    }

    override fun mayBeDiscontinuousImpl(): Boolean {
        return !isFunctionContinuous || argument.mayBeDiscontinuous()
    }

    open fun rewriteToSimplifyArgument(): Expression {
        return this
    }

    final override fun withMembers(members: List<Expression>): Expression {
        val argument = members.single()
        return this::class.primaryConstructor!!.call(argument)
    }

    final override fun differentiate(variable: String): Expression {
        return argument.differentiate(variable) * functionDerivative(argument)
    }

    fun asFunction(): PFunctionFromExpression {
        return this::class.primaryConstructor!!.call(v("x")).."x"
    }

    final override fun toString(): String {
        return "$name($argument)"
    }

    final override fun equals(other: Any?): Boolean {
        return other is NativeFunction
                && other::class == this::class
                && other.name == name
                && other.outDomain == outDomain
                && other.argument == argument
    }

    final override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + this::class.hashCode()
        result = 31 * result + this.outDomain.hashCode()
        result = 31 * result + this.argument.hashCode()
        return result
    }
}

class Id(argument: Expression): NativeFunction(argument) {
    override val name: String = "id"
    override val outDomain: Quantity<PDouble> = AnyQuantity()
    override val functionDerivative: PFunctionFromExpression = c(1).."x"
    override val reciprocal: PFunctionFromExpression = Id(v("x")).."x"
    override val isFunctionContinuous: Boolean = true
    override fun invoke(x: PDouble): PDouble = x
    override fun invoke(x: PDoubleInterval): Quantity<PDouble> = x
}


class Exp(argument: Expression) : NativeFunction(argument) {
    override val name: String = "exp"
    override val outDomain: Quantity<PDouble> = PDoubleInterval.Builtin.strictlyPositive
    override val isFunctionContinuous: Boolean = true
    override val functionDerivative: PFunctionFromExpression get() = Exp(Var("x")).."x"
    override val reciprocal: PFunctionFromExpression get() = Ln(Var("x")).."x"

    override fun simplifyImpl(): Expression {
        if (argument is Prod && argument.members.any { it is Ln }) {
            val ln = argument.members.first { it is Ln } as Ln
            val remaining = argument.members.filter { it !== ln }
            return ln.argument.pow(Prod(remaining))
        }
        return super.simplifyImpl()
    }

    override fun invoke(x: PDouble): PDouble {
        return x.applyFunction(::exp)
    }

    override fun invoke(x: PDoubleInterval): Quantity<PDouble> {
        return x.applyMonotonousFunction(::exp)
    }

    fun withNegatedExponent(): Expression {
        return Exp(-argument)
    }
}

class Ln(argument: Expression) : NativeFunction(argument) {
    override val name: String = "ln"
    override val outDomain: Quantity<PDouble> = AnyQuantity()
    override val isFunctionContinuous: Boolean = true
    override val functionDerivative: PFunctionFromExpression get() = (Const(1)/Var("x")).."x"
    override val reciprocal: PFunctionFromExpression get() = Exp(Var("x")).."x"

    override fun rewriteToSimplifyArgument(): Expression {
        val simplifiedArgument = argument.simplify()
        return if (simplifiedArgument is Pow) simplifiedArgument.exponent * Ln(simplifiedArgument.x).apply { assertSimplified() }
        else this
    }

    override fun invoke(x: PDouble): PDouble {
        return x.applyFunction(::ln)
    }

    override fun invoke(x: PDoubleInterval): Quantity<PDouble> {
        return x.applyMonotonousFunction(::ln)
    }
}

class Sin(argument: Expression) : NativeFunction(argument) {
    override val name: String = "sin"
    override val outDomain: Quantity<PDouble> = PDoubleInterval.Builtin.fromMinus1To1
    override val isFunctionContinuous: Boolean = true
    override val functionDerivative: PFunctionFromExpression get() = Cos(Var("x")).."x"
    override val reciprocal: PFunctionFromExpression = Arcsin(Var("x")).."x"

    override fun rewriteToSimplifyArgument(): Expression {
        val simplifiedArgument = argument.simplify()
        if (simplifiedArgument is Minus) {
            return -Sin(simplifiedArgument.value)
        }
        return this
    }

    override fun invoke(x: PDouble): PDouble {
        return x.applyFunction(::sin)
    }

    override fun invoke(x: PDoubleInterval): Quantity<PDouble> {
        return x.applyPeriodicalFunction(::sin, t = PDouble(2*PI), mapOf(PDouble(PI/2) to PDouble(1), PDouble(3*PI/2) to PDouble(-1)))
    }
}

class Cos(argument: Expression) : NativeFunction(argument) {
    override val name: String = "cos"
    override val outDomain: Quantity<PDouble> = PDoubleInterval.Builtin.fromMinus1To1
    override val isFunctionContinuous: Boolean = true
    override val functionDerivative: PFunctionFromExpression get() = (-Sin(v("x"))).."x"
    override val reciprocal: PFunctionFromExpression = Arccos(v("x")).."x"

    override fun rewriteToSimplifyArgument(): Expression {
        return when (val simplifiedArgument = argument.simplify()) {
            is Minus -> Cos(simplifiedArgument.value)
            is Sub -> {
                val left = simplifiedArgument.left
                if (left is Const && left.value == PDouble(PI/2)) Sin(simplifiedArgument.right) else this
            }
            else -> this
        }
    }

    override fun invoke(x: PDouble): PDouble {
        return x.applyFunction(::cos)
    }

    override fun invoke(x: PDoubleInterval): Quantity<PDouble> {
        return x.applyPeriodicalFunction(::cos, t = PDouble(2*PI), mapOf(PDouble(0) to PDouble(1), PDouble(PI) to PDouble(-1)))
    }
}

class Arcsin(argument: Expression) : NativeFunction(argument) {
    override val name: String = "arcsin"
    override val outDomain: Quantity<PDouble> = PDoubleInterval.Builtin.fromMinus1To1
    override val isFunctionContinuous: Boolean = true
    override val functionDerivative: PFunctionFromExpression = (c(1) / sqrt( v("x").square() + c(1) )).."x"
    override val reciprocal: PFunctionFromExpression get() = Sin(Var("x")).."x"

    override fun invoke(x: PDouble): PDouble {
        return x.applyFunction(::asin)
    }

    override fun invoke(x: PDoubleInterval): Quantity<PDouble> {
        return x.applyPeriodicalFunction(::asin, t = PDouble(2*PI), mapOf(PDouble(0) to PDouble(1), PDouble(PI) to PDouble(-1)))
    }
}

class Arccos(argument: Expression) : NativeFunction(argument) {
    override val name: String = "arccos"
    override val outDomain: Quantity<PDouble> = PDoubleInterval.Builtin.fromMinus1To1
    override val isFunctionContinuous: Boolean = true
    override val functionDerivative: PFunctionFromExpression = (c(-1) / sqrt( v("x").square() + c(1) )).."x"
    override val reciprocal: PFunctionFromExpression get() = Cos(Var("x")).."x"

    override fun invoke(x: PDouble): PDouble {
        return x.applyFunction(::acos)
    }

    override fun invoke(x: PDoubleInterval): Quantity<PDouble> {
        return x.applyMonotonousFunction(::acos)
    }
}

@Suppress("FunctionName")
fun Tan(x: Expression) = Sin(x)/Cos(x)
