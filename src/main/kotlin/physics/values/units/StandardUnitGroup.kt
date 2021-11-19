package physics.values.units


class StandardUnitGroup(measuredQuantity: String, mainUnit: String) : UnitGroup(measuredQuantity, mainUnit) {
    fun getConvertingCoefficient(unit: String, target: String): Double? {
        return getConvertingCoefficientImpl(unit, target)
    }
}
