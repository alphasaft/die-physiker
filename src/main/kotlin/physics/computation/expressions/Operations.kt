package physics.computation.expressions

operator fun Expression.plus(other: Expression) = Sum(this, other).simplify()
operator fun Expression.minus(other: Expression) = Sub(this, other).simplify()
operator fun Expression.times(other: Expression) = Prod(this, other).simplify()
operator fun Expression.div(other: Expression) = Div(this, other).simplify()
operator fun Expression.unaryMinus() = Minus(this).simplify()

// TODO : Define something like a withPhysicalDoubleFactory(it) { ... }
// TODO : Make a more general 'Function' class including Exp and Ln.

class OperationsScope {

}

fun Expression.square() = pow(Const(0.5))
fun Expression.pow(other: Expression) = Pow(this, other).simplify()
fun log(x: Expression, base: Expression = Const(10)) = Log(x, base).simplify()
fun ln(x: Expression) = Ln(x).simplify()
fun exp(x: Expression) = Exp(x).simplify()
fun root(x: Expression, exponent: Expression = Const(2)) = Root(x, exponent).simplify()
