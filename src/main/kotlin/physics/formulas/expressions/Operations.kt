package physics.formulas.expressions

operator fun Expression.plus(other: Expression) = Sum(this, other).simplify()
operator fun Expression.minus(other: Expression) = Sub(this, other).simplify()
operator fun Expression.times(other: Expression) = Prod(this, other).simplify()
operator fun Expression.div(other: Expression) = Div(this, other).simplify()
operator fun Expression.unaryMinus() = Minus(this).simplify()

fun Expression.square() = pow(Const(2))
fun Expression.pow(other: Expression) = Pow(this, other).simplify()
fun log(x: Expression, base: Expression = Const(10)) = Log(x, base).simplify()
fun root(x: Expression, exponent: Expression = Const(2)) = Root(x, exponent).simplify()
