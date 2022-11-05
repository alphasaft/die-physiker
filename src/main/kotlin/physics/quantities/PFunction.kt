package physics.quantities


interface PFunction  {
    val outDomain: Quantity<PDouble>

    val reciprocal: PFunction
    val derivative: PFunction

    operator fun invoke(x: String): String
    operator fun invoke(x: PDouble): PDouble
    fun invokeExhaustively(x: Quantity<PDouble>): Quantity<PDouble>
}

