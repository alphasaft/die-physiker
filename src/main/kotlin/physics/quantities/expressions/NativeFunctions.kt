package physics.quantities.expressions

import Args
import physics.quantities.*
import physics.quantities.doubles.*
import physics.quantities.doubles.Function
import kotlin.math.*
import kotlin.reflect.full.primaryConstructor


abstract class NativeFunction(val argument: Expression) : Expression() {
    final override val members: Collection<Expression> = listOf(argument)

    abstract val name: String
    abstract override val outDomain: Quantity<PReal>
    abstract val isFunctionContinuous: Boolean
    abstract val functionDerivative: ExpressionAsFunction
    abstract val reciprocal: ExpressionAsFunction

    private val associatedFunction = object : Function {
        override val outDomain: Quantity<PReal> get() = this@NativeFunction.outDomain
        override val reciprocal: Function get() = this@NativeFunction.reciprocal
        override fun invoke(x: String): String = "$name($x)"
        override fun invoke(x: PReal): PReal = this@NativeFunction(x)
        override fun invokeExhaustively(x: Quantity<PReal>): Quantity<PReal> = this@NativeFunction(x)
    }

    protected abstract operator fun invoke(x: PReal): PReal
    protected abstract operator fun invoke(x: PRealInterval): Quantity<PReal>

    operator fun invoke(x: Quantity<PReal>): Quantity<PReal> {
        return when (x) {
            is PReal -> this(x)
            is PRealInterval -> this(x)
            else -> AnyQuantity()
        }
    }

    final override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int>): Quantity<PReal> {
        return argument.evaluateExhaustively(arguments, counters).applyFunction(associatedFunction)
    }

    final override fun evaluate(arguments: Args<VariableValue<PReal>>, counters: Args<Int>): PReal {
        return this(argument.evaluate(arguments, counters)).asPValue<PReal>()
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

    final override fun derive(variable: String): Expression {
        return argument.derive(variable) * functionDerivative(argument)
    }

    fun asFunction(): ExpressionAsFunction {
        return this::class.primaryConstructor!!.call(v("x")).asFunction("x")
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
    override val outDomain: Quantity<PReal> = AnyQuantity()
    override val functionDerivative: ExpressionAsFunction = c(1).asFunction("x")
    override val reciprocal: ExpressionAsFunction = Id(v("x")).asFunction("x")
    override val isFunctionContinuous: Boolean = true
    override fun invoke(x: PReal): PReal = x
    override fun invoke(x: PRealInterval): Quantity<PReal> = x
}


class Exp(argument: Expression) : NativeFunction(argument) {
    override val name: String = "exp"
    override val outDomain: Quantity<PReal> = PRealInterval.Builtin.strictlyPositive
    override val isFunctionContinuous: Boolean = true
    override val functionDerivative: ExpressionAsFunction get() = Exp(Var("x")).asFunction("x")
    override val reciprocal: ExpressionAsFunction get() = Ln(Var("x")).asFunction("x")

    override fun simplifyImpl(): Expression {
        if (argument is Prod && argument.members.any { it is Ln }) {
            val ln = argument.members.first { it is Ln } as Ln
            val remaining = argument.members.filter { it !== ln }
            return ln.argument.pow(Prod(remaining))
        }
        return super.simplifyImpl()
    }

    override fun invoke(x: PReal): PReal {
        return x.applyFunction(::exp)
    }

    override fun invoke(x: PRealInterval): Quantity<PReal> {
        return x.applyMonotonousFunction(::exp)
    }

    fun withNegatedExponent(): Expression {
        return Exp(-argument)
    }
}

class Ln(argument: Expression) : NativeFunction(argument) {
    override val name: String = "ln"
    override val outDomain: Quantity<PReal> = AnyQuantity()
    override val isFunctionContinuous: Boolean = true
    override val functionDerivative: ExpressionAsFunction get() = (Const(1)/Var("x")).asFunction("x")
    override val reciprocal: ExpressionAsFunction get() = Exp(Var("x")).asFunction("x")

    override fun rewriteToSimplifyArgument(): Expression {
        val simplifiedArgument = argument.simplify()
        return if (simplifiedArgument is Pow) simplifiedArgument.exponent * Ln(simplifiedArgument.x).apply { assertSimplified() }
        else this
    }

    override fun invoke(x: PReal): PReal {
        return x.applyFunction(::ln)
    }

    override fun invoke(x: PRealInterval): Quantity<PReal> {
        return x.applyMonotonousFunction(::ln)
    }
}

class Sin(argument: Expression) : NativeFunction(argument) {
    override val name: String = "sin"
    override val outDomain: Quantity<PReal> = PRealInterval.Builtin.fromMinus1To1
    override val isFunctionContinuous: Boolean = true
    override val functionDerivative: ExpressionAsFunction = Cos(Var("x")).asFunction("x")
    override val reciprocal: ExpressionAsFunction = Arcsin(Var("x")).asFunction("x")

    override fun rewriteToSimplifyArgument(): Expression {
        val simplifiedArgument = argument.simplify()
        if (simplifiedArgument is Minus) {
            return -Sin(simplifiedArgument.value)
        }
        return this
    }

    override fun invoke(x: PReal): PReal {
        return x.applyFunction(::sin)
    }

    override fun invoke(x: PRealInterval): Quantity<PReal> {
        return x.applyPeriodicalFunction(::sin, t = PReal(2*PI), mapOf(PReal(PI/2) to PReal(1), PReal(3*PI/2) to PReal(-1)))
    }
}

class Cos(argument: Expression) : NativeFunction(argument) {
    override val name: String = "cos"
    override val outDomain: Quantity<PReal> = PRealInterval.Builtin.fromMinus1To1
    override val isFunctionContinuous: Boolean = true
    override val functionDerivative: ExpressionAsFunction = (-Sin(v("x"))).asFunction("x")
    override val reciprocal: ExpressionAsFunction = Arccos(v("x")).asFunction("x")

    override fun rewriteToSimplifyArgument(): Expression {
        return when (val simplifiedArgument = argument.simplify()) {
            is Minus -> Cos(simplifiedArgument.value)
            is Sub -> {
                val left = simplifiedArgument.left
                if (left is Const && left.value == PReal(PI/2)) Sin(simplifiedArgument.right) else this
            }
            else -> this
        }
    }

    override fun invoke(x: PReal): PReal {
        return x.applyFunction(::sin)
    }

    override fun invoke(x: PRealInterval): Quantity<PReal> {
        return x.applyPeriodicalFunction(::cos, t = PReal(2*PI), mapOf(PReal(0) to PReal(1), PReal(PI) to PReal(-1)))
    }
}

class Arcsin(argument: Expression) : NativeFunction(argument) {
    override val name: String = "arcsin"
    override val outDomain: Quantity<PReal> = PRealInterval.Builtin.fromMinus1To1
    override val isFunctionContinuous: Boolean = true
    override val functionDerivative: ExpressionAsFunction = (c(1) / sqrt( v("x").square() + c(1) )).asFunction("x")
    override val reciprocal: ExpressionAsFunction = Sin(Var("x")).asFunction("x")

    override fun rewriteToSimplifyArgument(): Expression {
        val simplifiedArgument = argument.simplify()
        if (simplifiedArgument is Minus) {
            return Cos(simplifiedArgument.value)
        }
        return this
    }

    override fun invoke(x: PReal): PReal {
        return x.applyFunction(::sin)
    }

    override fun invoke(x: PRealInterval): Quantity<PReal> {
        return x.applyPeriodicalFunction(::cos, t = PReal(2*PI), mapOf(PReal(0) to PReal(1), PReal(PI) to PReal(-1)))
    }
}

class Arccos(argument: Expression) : NativeFunction(argument) {
    override val name: String = "arccos"
    override val outDomain: Quantity<PReal> = PRealInterval.Builtin.fromMinus1To1
    override val isFunctionContinuous: Boolean = true
    override val functionDerivative: ExpressionAsFunction = (c(-1) / sqrt( v("x").square() + c(1) )).asFunction("x")
    override val reciprocal: ExpressionAsFunction = Cos(Var("x")).asFunction("x")

    override fun rewriteToSimplifyArgument(): Expression {
        val simplifiedArgument = argument.simplify()
        if (simplifiedArgument is Minus) {
            return Cos(simplifiedArgument.value)
        }
        return this
    }

    override fun invoke(x: PReal): PReal {
        return x.applyFunction(::sin)
    }

    override fun invoke(x: PRealInterval): Quantity<PReal> {
        return x.applyPeriodicalFunction(::cos, t = PReal(2*PI), mapOf(PReal(0) to PReal(1), PReal(PI) to PReal(-1)))
    }
}
