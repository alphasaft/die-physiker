package physics.quantities

import kotlin.reflect.KClass


class PBoolean(val value: Boolean) : PValue<PBoolean>() {
    override val type: KClass<PBoolean> = PBoolean::class

    override fun toString(): String {
        return value.toString()
    }

    override fun equals(other: Any?): Boolean {
        return other is PBoolean && value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toPBoolean(): PBoolean = this
    override fun toPInt(): PInt = PInt(if (value) 1 else 0)
    override fun toPReal(): PReal = PReal(if (value) 1.0 else 0.0)
    override fun toPString(): PString = PString(value.toString())

    fun not(): PBoolean {
        return PBoolean(!value)
    }

}
