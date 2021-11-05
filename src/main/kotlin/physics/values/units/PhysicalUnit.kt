package physics.values.units

import mergeWith
import mergedWith
import Couple
import physics.AmbiguousUnitException
import physics.ConversionNeededException
import physics.IncompatibleUnitsException
import toInt

import kotlin.math.pow


class PhysicalUnit(private val signature: UnitSignature) {
    constructor(symbols: String): this(kotlin.run {
        if (symbols.isBlank()) return@run emptyMap()

        val regex = Regex("^(\\w+?)(-?\\d+)?$")
        val signature = mutableMapOf<String, Int>()
        for (symbol in symbols.split(".")) {
            val match = requireNotNull(regex.matchEntire(symbol))
            val name = match.groupValues[1]
            val exponent = match.groups[2]?.value?.toInt() ?: 1
            signature[name] = exponent
        }
        return@run signature
    })
    constructor(): this(emptyMap())

    init {
        if (signature.keys.any { unit1 -> (signature.keys - unit1).any { unit2 -> isConvertible(PhysicalUnit(unit1), PhysicalUnit(unit2)) } }) {
            throw AmbiguousUnitException(this)
        }
    }

    fun isNeutral() = signature.isEmpty()

    operator fun times(other: PhysicalUnit): PhysicalUnit {
        return when {
            this.isNeutral() -> other
            other.isNeutral() -> this
            other != this && isConvertible(this, other) -> throw ConversionNeededException(other, this)
            else -> PhysicalUnit(signature
                .mergedWith(other.signature, merge = Int::plus)
                .filterValues { it != 0 })
        }
    }

    operator fun div(other: PhysicalUnit): PhysicalUnit {
        if (other != this && isConvertible(this, other)) throw ConversionNeededException(other, this)

        return PhysicalUnit(signature
            .mergedWith(other.signature.mapValues { (_, exponent) -> -exponent }, merge = Int::plus)
            .filterValues { it != 0 })
    }

    operator fun plus(other: PhysicalUnit): PhysicalUnit {
        when {
            signature == other.signature || isNeutral() || other.isNeutral() -> return this
            isConvertible(this, other) -> throw ConversionNeededException(other, this)
            else -> throw IncompatibleUnitsException(this, other)
        }
    }

    operator fun minus(other: PhysicalUnit): PhysicalUnit = this + other

    fun convertInto(other: String) = convertInto(PhysicalUnit(other))
    fun convertInto(other: UnitSignature) = convertInto(PhysicalUnit(other))
    fun convertInto(other: PhysicalUnit): PhysicalUnit {
        if (!isConvertible(this, other)) throw IncompatibleUnitsException(this, other)
        return other
    }

    override fun equals(other: Any?): Boolean {
        return other is PhysicalUnit && other.signature == this.signature
    }

    override fun hashCode(): Int {
        return signature.hashCode()
    }

    override fun toString(): String {
        val orderedSignature = signature.toList().sortedBy { it.second + 100 * (it.second < 0).toInt() }
        return orderedSignature.joinToString(".") { (symbol, exponent) -> if (exponent != 1) "$symbol$exponent" else symbol }
    }

    companion object Register {
        private val converters = mutableMapOf<Couple<String>, Double>()
        private val convertingCache = mutableMapOf<Couple<UnitSignature>, Double>()
        private val aliases = mutableMapOf<String, UnitSignature>()

        fun addConverter(from: String, to: String, coefficient: Double) {
            converters[Pair(from, to)] = coefficient
            chainConvertersWith(from, to, coefficient)
            converters[Pair(to, from)] = 1.0 / coefficient
        }

        private fun chainConvertersWith(from: String, to: String, coefficient: Double) {
            for ((converterId, converterCoefficient) in converters) {
                if (converterId.second == from) {
                    converters[Pair(converterId.first, to)] = coefficient * converterCoefficient
                }
            }
        }

        fun addAlias(name: String, signature: UnitSignature) {
            aliases[name] = signature
        }

        fun isConvertible(unit: PhysicalUnit, into: PhysicalUnit): Boolean =
            convert(unit, into, 0.0) == 0.0

        fun convert(from: PhysicalUnit, to: PhysicalUnit, initialValue: Double): Double? {
            val initialSignature = from.signature
            val targetSignature = to.signature

            if (initialSignature == targetSignature || from.isNeutral() || to.isNeutral()) return initialValue
            if (Pair(initialSignature, targetSignature) in convertingCache) return convertingCache.getValue(Pair(initialSignature, targetSignature)) * initialValue

            var value = initialValue
            val (flattenedInitialSignature, flatteningCoefficientOfSource) = flattenAliasesOf(initialSignature)
            val (flattenedTargetSignature, flatteningCoefficientOfTarget) = flattenAliasesOf(targetSignature)

            if (flattenedInitialSignature.size != flattenedTargetSignature.size) return null  // Can't succeed

            value *= flatteningCoefficientOfSource / flatteningCoefficientOfTarget

            for ((unit, exponent) in flattenedInitialSignature) {
                if (unit in flattenedTargetSignature && exponent == flattenedTargetSignature.getValue(unit)) continue

                val correspondingUnits = flattenedTargetSignature.filter { (targetUnit, targetExponent) -> Pair(unit, targetUnit) in converters && targetExponent == exponent }
                if (correspondingUnits.isEmpty()) return null
                val correspondingUnit = correspondingUnits.toList().single().first
                val converterCoefficient = converters.getValue(unit to correspondingUnit)
                value *= converterCoefficient.pow(exponent)
            }

            convertingCache[Pair(flattenedInitialSignature, flattenedTargetSignature)] = value / initialValue
            convertingCache[Pair(flattenedTargetSignature, flattenedInitialSignature)] = initialValue / value
            convertingCache[Pair(initialSignature, targetSignature)] = value / initialValue
            convertingCache[Pair(targetSignature, initialSignature)] = initialValue / value

            return value
        }

        private fun flattenAliasesOf(signature: UnitSignature): Pair<UnitSignature, Double> {
            val resultingSignature = signature.toMutableMap()
            var overallCoefficient = 1.0

            for ((aliasName, aliasContent) in aliases) {
                val fullAliasesSet = converters
                    .filter { (k, _) -> aliasName == k.first }
                    .map { (k, v) -> k.second to 1.0/v }
                    .plusElement(Pair(aliasName, 1.0))

                for ((subAlias, convertingCoefficient) in fullAliasesSet) {
                    if (signature.containsKey(subAlias)) {
                        val exponent = signature.getValue(subAlias)
                        resultingSignature.mergeWith(aliasContent.mapValues { (_, v) -> v * exponent }, Int::plus)
                        resultingSignature.remove(subAlias)
                        overallCoefficient *= convertingCoefficient.pow(exponent)
                    }
                }
            }

            return resultingSignature to overallCoefficient
        }
    }
}
