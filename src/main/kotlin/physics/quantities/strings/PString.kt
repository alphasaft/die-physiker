package physics.quantities.strings

import physics.quantities.PValue
import physics.quantities.doubles.PReal
import physics.quantities.booleans.PBoolean
import physics.quantities.ints.PInt
import kotlin.reflect.KClass


class PString(val value: String): PValue<PString>(), CharSequence by value {
    override val type: KClass<PString> = PString::class

    override fun toPBoolean(): PBoolean = PBoolean(value == "true")
    override fun toPInt() = PInt(value.toInt())
    override fun toPReal() = PReal(value.toDouble())
    override fun toPString(): PString = this

    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        return other is PString && other.value == value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}