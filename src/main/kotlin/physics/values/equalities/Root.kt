package physics.values.equalities

class Root(x: Expression, private val originalExponent: Expression = Const(2)) : Pow(x, Div(Const(1), originalExponent)) {
    override fun toString(): String {
        return if (originalExponent == Const(2)) "sqrt($x)"
        else "rt<$originalExponent>($x)"
    }
}
