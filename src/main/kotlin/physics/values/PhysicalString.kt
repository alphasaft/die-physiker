package physics.values

import Mapper
import Predicate
import physics.alwaysTrue
import physics.noop
import physics.values.units.UnitScope
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass


open class PhysicalString(
    override val value: String,
    private val unitScope: UnitScope,
): PhysicalValue<String>, CharSequence by value {

    override fun toPhysicalString() = this
    override fun toPhysicalDouble() = PhysicalValuesFactory(unitScope).double(value)
    override fun toPhysicalInt() = PhysicalInt(value.toInt(), unitScope)

    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        return other is PhysicalString && other.value == value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}