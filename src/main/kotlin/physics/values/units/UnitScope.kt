package physics.values.units

sealed class UnitScope {
    fun convert(from: PhysicalUnit, to: PhysicalUnit, initialValue: Double): Double? {
        require(from.getScope() === this)  { "Can't convert a unit from another scope." }
        return convertImpl(from, to, initialValue)
    }

    abstract fun convertImpl(unit1: PhysicalUnit, unit2: PhysicalUnit, initialValue: Double): Double?

    fun isConvertible(unit1: PhysicalUnit, unit2: PhysicalUnit): Boolean {
        return convert(unit1, unit2, 0.0) != null
    }

    fun physicalUnit(signature: String = "") = PhysicalUnit(this, signature)
}
