package physics.quantities.units

import Couple

sealed class UnitGroup(private val measuredQuantity: String, mainUnit: String, secondaryUnits: Map<String, Double>) {
    private val unitsCoefficients = mapOf(mainUnit to 1.0) + secondaryUnits
    private val converters = mutableMapOf<Couple<String>, Double>()

    fun convert(unit: String, target: String, value: Double): Double? {
        val coefficient = getConvertingCoefficientImpl(unit, target) ?: return null
        return value * coefficient
    }

    protected fun getConvertingCoefficientImpl(unit: String, target: String): Double? {
        if (unit !in this || target !in this) return null
        return unitsCoefficients.getValue(unit)/unitsCoefficients.getValue(target)
    }

    operator fun contains(unit: String): Boolean {
        return unit in unitsCoefficients
    }
}