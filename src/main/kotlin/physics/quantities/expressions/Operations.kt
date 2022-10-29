package physics.quantities.expressions

import physics.quantities.PDouble

fun v(name: String): Var = Var(name)
fun c(value: Int): Const = Const(value)
fun c(value: Double): Const = Const(value)
fun c(value: PDouble): Const = Const(value)

operator fun Expression.plus(other: Expression) = Sum(this, other).simplify()
operator fun Expression.minus(other: Expression) = Sub(this, other).simplify()
operator fun Expression.times(other: Expression) = Prod(this, other).simplify()
operator fun Expression.div(other: Expression) = Div(this, other).simplify()
operator fun Expression.unaryMinus() = Minus(this).simplify()

fun log(x: Expression, base: Expression = Const(10)) = Log(x, base).simplify()
fun ln(x: Expression) = Ln(x).simplify()
fun exp(x: Expression) = Exp(x).simplify()
fun root(x: Expression, exponent: Expression) = Root(x, exponent).simplify()
fun sqrt(x: Expression) = root(x, Const(2))

fun Expression.square() = pow(Const(2))
fun Expression.pow(other: Expression) = Pow(this, other).simplify()
fun Expression.where(vararg bindings: Pair<String, Expression>): Expression = WhereBindings(this, bindings.toMap())

infix fun Expression.equals(other: Expression) = Equation(this.simplify(), other.simplify())
infix fun String.equals(other: Expression) = Equation(Var(this), other)

