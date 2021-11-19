package physics.values.units


internal object NeutralUnitScope : UnitScope() {
    override fun convertImpl(
        unit1: PhysicalUnit,
        unit2: PhysicalUnit,
        initialValue: Double
    ): Double {
        // always succeed
        return initialValue
    }
}