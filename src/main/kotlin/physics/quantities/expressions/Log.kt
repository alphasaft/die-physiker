package physics.quantities.expressions


class Log(val x: Expression, val base: Expression = Const(10)) : Alias(Ln(x)/Ln(base)) {
    override fun toString(): String {
        return when (base) {
            Const(10) -> "log($x)"
            else -> "log<$base>($x)"
        }
    }
}