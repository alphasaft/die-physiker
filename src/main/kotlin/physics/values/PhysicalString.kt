package physics.values

import Mapper
import Predicate
import physics.alwaysTrue
import physics.noop
import physics.units.PhysicalUnit
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass


open class PhysicalString(override val value: String): PhysicalValue<String>, CharSequence by value {
    override fun toPhysicalInt() = PhysicalInt(value.toInt())
    override fun toPhysicalDouble() = PhysicalDouble.withUnit(PhysicalUnit()).fromString(value)
    override fun toPhysicalString() = this

    override fun toString(): String {
        return value
    }

    companion object FactoryProvider {
        private class Factory(
            val normalizer: Mapper<String>,
            val check: Predicate<String>,
        ) : PhysicalValue.Factory<PhysicalString> {

            override val of: KClass<PhysicalString> = PhysicalString::class
            override fun fromString(value: String): PhysicalString {
                if (!check(value)) throw IllegalArgumentException("String '$value' doesn't satisfy the constraints posed on this PhysicalString.")
                return PhysicalString(normalizer(value))
            }
        }

        fun any(): PhysicalValue.Factory<PhysicalString> = model()
        fun model(normalizer: Mapper<String> = ::noop, check: Predicate<String> = ::alwaysTrue): PhysicalValue.Factory<PhysicalString> = Factory(normalizer, check)
    }
}