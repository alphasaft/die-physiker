package physics.values.units

import Couple
import mergeWith
import mergedWith
import physics.UnitException
import physics.quantities.doubles.PreciseDouble
import kotlin.math.pow


class PUnit(private val signature: UnitSignature) {

    constructor(): this(emptyMap())

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

    private companion object Scope {
        private val convertingCache = mutableMapOf<Couple<UnitSignature>, PreciseDouble>()
        private val unitGroups = listOf<PlainUnitGroup>()
        private val aliasesGroups = listOf<UnitAliasGroup>()
        private val neutralSignatures = listOf<UnitSignature>(emptyMap())

        fun convert(unit1: PUnit, unit2: PUnit, initialValue: PreciseDouble): PreciseDouble? {
            val initialSignature = unit1.signature
            val targetSignature = unit2.signature

            if (initialSignature == targetSignature || unit1.isNeutral() || unit2.isNeutral()) return initialValue
            if (Pair(initialSignature, targetSignature) in convertingCache)
                return convertingCache.getValue(Pair(initialSignature, targetSignature)) * initialValue

            var value = initialValue
            val (flattenedInitialSignature, flatteningCoefficientOfSource) = flattenAliasesOf(initialSignature)
            val (flattenedTargetSignature, flatteningCoefficientOfTarget) = flattenAliasesOf(targetSignature)
            if (flattenedInitialSignature.size != flattenedTargetSignature.size) return null  // Can't succeed

            value *= flatteningCoefficientOfSource / flatteningCoefficientOfTarget

            for ((unit, exponent) in flattenedInitialSignature) {
                val correspondingUnitGroup = unitGroups.firstOrNull { unit in it } ?: throw UnitException("Unit '$unit' wasn't declared.")
                val target = findCompatibleUnitIn(flattenedTargetSignature.keys, unit) ?: return null
                value *= correspondingUnitGroup.getConvertingCoefficient(unit, target)!!.pow(exponent)
            }

            convertingCache[Pair(flattenedInitialSignature, flattenedTargetSignature)] = value / initialValue
            convertingCache[Pair(flattenedTargetSignature, flattenedInitialSignature)] = initialValue / value
            convertingCache[Pair(initialSignature, targetSignature)] = value / initialValue
            convertingCache[Pair(targetSignature, initialSignature)] = initialValue / value

            return value
        }

        private fun findCompatibleUnitIn(availableUnits: Set<String>, target: String): String? {
            val matchingUnits = availableUnits.filter { (unitGroups + aliasesGroups).any { g -> it in g && target in g } }
            if (matchingUnits.size > 1) throw UnitException("Units '${matchingUnits.joinToString(", ")} are ambiguous.")
            return matchingUnits.singleOrNull()
        }

        private fun flattenAliasesOf(signature: UnitSignature): Pair<UnitSignature, Double> {
            val result = signature.toMutableMap()
            var overallCoefficient = 1.0
            for ((unit, exponent) in signature) {
                if (aliasesGroups.none { unit in it }) continue
                val aliasGroup = aliasesGroups.single { unit in it }
                val (aliasSignature, coefficient) = aliasGroup.flattenAlias(unit)!!
                overallCoefficient *= coefficient
                result.remove(unit)
                result.mergeWith(aliasSignature.mapValues { (_, v) -> v * exponent }, merge = Int::plus)
            }
            return result to overallCoefficient
        }

        fun isConvertible(unit1: PUnit, unit2: PUnit): Boolean {
            return convert(unit1, unit2, PreciseDouble(0.0)) != null
        }
    }

    init {
        if (signature.keys.any { unit1 -> (signature.keys - unit1).any { unit2 -> isConvertible(PUnit(unit1), PUnit(unit2)) } }) {
            throw UnitException("Unit $this is ambiguous.")
        }
    }

    fun isNeutral() = signature in neutralSignatures

    operator fun plus(other: PUnit): PUnit {
        return when {
            isNeutral() -> other
            other.isNeutral() -> this
            signature == other.signature -> this
            isConvertible(this, other) -> throw UnitException("Unit $other should be converted into $this before adding them together.")
            else -> throw UnitException("Unit $this is not convertible into $other")
        }
    }

    private fun invert(): PUnit {
        return PUnit(signature.mapValues { (_, v) -> -v })
    }

    operator fun times(other: PUnit): PUnit {
        return when {
            this.isNeutral() -> other
            other.isNeutral() -> this
            other != this && isConvertible(this, other) -> throw UnitException("Unit $other should be converted into $this before multiplying both together.")
            else -> PUnit(signature
                .mergedWith(other.signature, merge = Int::plus)
                .filterValues { it != 0 })
        }
    }

    operator fun div(other: PUnit): PUnit {
        return this * other.invert()
    }

    fun convert(value: PreciseDouble, into: PUnit): PreciseDouble? = convert(this, into, value)
    fun isConvertibleInto(unit: PUnit) = isConvertible(this, unit)

    override fun equals(other: Any?): Boolean {
        return other is PUnit && other.signature == this.signature
    }

    override fun hashCode(): Int {
        return signature.hashCode()
    }

    override fun toString(): String {
        val orderedSignature = signature.toList().sortedBy { it.second + 100 * (if (it.second < 0) 1 else 0) }
        return orderedSignature.joinToString(".") { (symbol, exponent) -> if (exponent != 1) "$symbol$exponent" else symbol }
    }
}
