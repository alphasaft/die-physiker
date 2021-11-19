package physics.values

import Mapper
import Predicate
import physics.alwaysTrue
import physics.noop
import physics.values.units.NeutralUnitScope
import physics.values.units.PhysicalUnit
import physics.values.units.UnitScope
import java.lang.IllegalArgumentException
import kotlin.math.pow
import kotlin.reflect.KClass

open class PhysicalValuesFactory protected constructor(private val unitScope: UnitScope) {
    companion object Constructor {
        private val cache = mutableMapOf<UnitScope, PhysicalValuesFactory>()

        operator fun invoke(unitScope: UnitScope): PhysicalValuesFactory {
            return cache[unitScope] ?: PhysicalValuesFactory(unitScope).also { cache[unitScope] = it }
        }
    }

    private inner class PhysicalIntFactory : PhysicalValue.Factory<PhysicalInt> {
        override val of: KClass<PhysicalInt> = PhysicalInt::class
        override fun fromString(value: String): PhysicalInt {
            return PhysicalInt(value.toInt(), unitScope)
        }
    }

    fun int(value: Int) = PhysicalInt(value, unitScope)
    fun intFactory(): PhysicalValue.Factory<PhysicalInt> = PhysicalIntFactory()


    private val physicalDoubleStringFormatRegex = run {
        val ws = "\\s*"
        val coefficient = "-?\\d+(.\\d+)?"
        val exponent = "(\\*${ws}10$ws\\^|E)$ws(-?\\d+)"
        val unit = "\\w+(.\\w+(-?\\d+)?)*"
        Regex("($coefficient)$ws($exponent)?$ws($unit)?$ws")
    }

    fun double(exactValue: Double, significantDigitsCount: Int, unit: PhysicalUnit): PhysicalDouble =
        PhysicalDouble(exactValue, significantDigitsCount, unit)

    fun double(constant: Double): PhysicalDouble =
        PhysicalDouble(constant, Int.MAX_VALUE, unitScope.physicalUnit())

    fun double(exactValue: Double, significantDigitsCount: Int): PhysicalDouble =
        PhysicalDouble(exactValue, significantDigitsCount, unitScope.physicalUnit())

    fun double(constant: Double, unit: PhysicalUnit): PhysicalDouble =
        PhysicalDouble(constant, Int.MAX_VALUE, unit)

    fun double(value: String): PhysicalDouble {
        val match = physicalDoubleStringFormatRegex.matchEntire(value) ?: throw NumberFormatException("Invalid value : $value")
        val (coefficient, _, _, _, exponent, unit) = match.destructured
        return PhysicalDouble(
            coefficient.toDouble() * 10.0.pow(exponent.ifEmpty { "0" }.toInt()),
            coefficient.significantDigitsCount,
            (if (unit.isEmpty()) NeutralUnitScope else unitScope).physicalUnit(unit),
        )
    }

    fun doubleFactoryWithUnit(unit: PhysicalUnit): PhysicalValue.Factory<PhysicalDouble> = PhysicalDoubleFactory(unit)
    fun doubleFactoryWithUnit(unit: String): PhysicalValue.Factory<PhysicalDouble> = PhysicalDoubleFactory(unitScope.physicalUnit(unit))

    private inner class PhysicalDoubleFactory constructor(
        private val standardUnit: PhysicalUnit,
    ): PhysicalValue.Factory<PhysicalDouble> {
        override val of: KClass<PhysicalDouble> = PhysicalDouble::class

        override fun fromString(value: String): PhysicalDouble {
            return double(value).convertInto(standardUnit)
        }
    }


    fun string(value: String) = PhysicalString(value, unitScope)

    fun stringFactory(
        normalizer: Mapper<String> = ::noop,
        check: Predicate<String> = ::alwaysTrue
    ): PhysicalValue.Factory<PhysicalString> = StringFactory(normalizer, check)

    private inner class StringFactory(
        val normalizer: Mapper<String>,
        val check: Predicate<String>,
    ) : PhysicalValue.Factory<PhysicalString> {

        override val of: KClass<PhysicalString> = PhysicalString::class
        override fun fromString(value: String): PhysicalString {
            if (!check(value)) throw IllegalArgumentException("String '$value' doesn't satisfy the constraints posed on this PhysicalString.")
            return string(normalizer(value))
        }
    }
}