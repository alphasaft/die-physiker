package physics.quantities.doubles

import physics.quantities.Quantity

interface PRealOperand : Quantity<PReal> {
    operator fun unaryMinus(): Quantity<PReal>
    operator fun plus(other: PRealOperand): Quantity<PReal>
    operator fun minus(other: PRealOperand): Quantity<PReal> = this + (-other)
    operator fun times(other: PRealOperand): Quantity<PReal>
    operator fun div(other: PRealOperand): Quantity<PReal>
    fun pow(other: PRealOperand): Quantity<PReal>
}
