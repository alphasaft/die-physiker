package physics.values.units

import Couple
import println

sealed class UnitGroup(private val measuredQuantity: String, private val mainUnit: String) {
    internal val units = mutableMapOf(mainUnit to 1.0)
    private val converters = mutableMapOf<Couple<String>, Double>()

    fun addUnit(unit: String, convertingCoefficient: Double) {
        units[unit] = convertingCoefficient
    }

    fun convert(unit: String, target: String, value: Double): Double? {
        val coefficient = getConvertingCoefficientImpl(unit, target) ?: return null
        return value * coefficient
    }

    protected fun getConvertingCoefficientImpl(unit: String, target: String): Double? {
        if (unit !in units || target !in units) return null
        return units.getValue(unit)/units.getValue(target)
    }

    operator fun contains(unit: String): Boolean {
        return unit in units
    }
}