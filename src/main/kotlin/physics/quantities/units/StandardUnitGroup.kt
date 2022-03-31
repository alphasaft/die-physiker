package physics.quantities.units


class StandardUnitGroup(measuredQuantity: String, mainUnit: String, secondaryUnits: Map<String, Double>) : UnitGroup(measuredQuantity, mainUnit, secondaryUnits) {
    fun getConvertingCoefficient(unit: String, target: String): Double? {
        return getConvertingCoefficientImpl(unit, target)
    }
}
