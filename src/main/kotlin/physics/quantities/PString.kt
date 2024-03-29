package physics.quantities

import kotlin.reflect.KClass


class PString(override val value: String): PValue<PString>(), CharSequence by value {
    override val type: KClass<PString> = PString::class

    override fun toPBoolean(): PBoolean = PBoolean(value == "true")
    override fun toPInt() = PInt(value.toInt())
    override fun toPReal() = PDouble(value.toDouble())
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