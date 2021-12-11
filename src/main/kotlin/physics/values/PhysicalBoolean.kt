package physics.values

import physics.values.units.UnitScope


class PhysicalBoolean(
    override val value: Boolean,
    private val unitScope: UnitScope,
) : PhysicalValue<Boolean> {
    override fun toPhysicalDouble(): PhysicalDouble = toPhysicalInt().toPhysicalDouble()
    override fun toPhysicalInt(): PhysicalInt = PhysicalInt(value.toInt(), unitScope)
    override fun toPhysicalString(): PhysicalString = PhysicalString(value.toString(), unitScope)

    override fun toString(): String {
        return value.toString()
    }

    override fun equals(other: Any?): Boolean {
        return other is PhysicalBoolean && value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}