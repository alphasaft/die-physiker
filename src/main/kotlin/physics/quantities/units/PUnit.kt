package physics.quantities.units

import Couple
import isInt
import mergedWith
import physics.UnitException
import println
import kotlin.math.pow


class PUnit(private val signature: UnitSignature) {

    constructor(): this(emptyMap())

    constructor(symbols: String): this(kotlin.run {
        if (symbols.isBlank()) return@run emptyMap()

        val regex = Regex("^(\\w+?)\\^?(-?\\d+)?$")
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
        private val convertingCache = mutableMapOf<Couple<UnitSignature>, Double>()
        private val neutralSignatures = listOf(
            emptyMap(),
            mapOf("%" to 1),
            mapOf("dB" to 1)
        )

        private val unitGroups = listOf(
            PlainUnitGroup("temps", "s", generateUnits("s") + mapOf("min" to 60.0, "h" to 3600.0, "an" to 31557600.0)),
            PlainUnitGroup("distance", "m", generateUnits("m") + mapOf("UA" to 1.5E8, "AL" to 9.461E15)),
            PlainUnitGroup("masse", "kg", generateUnits("g").mapValues { (_, v) -> v*10E-3 } + mapOf("g" to 1E-3, "t" to 1E3, "Mt" to 1E9, "Gt" to 1E12)),
            PlainUnitGroup("quantité de matière", "mol", generateUnits("mol")),
            PlainUnitGroup("température", "K", generateUnits("K")),
            PlainUnitGroup("intensité lumineuse", "lux", generateUnits("lux")),
            PlainUnitGroup("débit électrique", "A", generateUnits("A"))
        )

        private val aliasesGroups = listOf(
            UnitAliasGroup("force", mapOf("kg" to 1, "m" to 1, "s" to -2), "N", generateUnits("N")),
            UnitAliasGroup("énergie", mapOf("N" to 1, "m" to 1), "J", generateUnits("J")),
            UnitAliasGroup("pression", mapOf("N" to 1, "m" to -2), "Pa", generateUnits("Pa")),
            UnitAliasGroup("puissance", mapOf("J" to 1, "s" to -1), "W", generateUnits("W")),
            UnitAliasGroup("tension", mapOf("W" to 1, "A" to -1), "V", generateUnits("V")),
            UnitAliasGroup("activité catalytique", mapOf("mol" to 1, "s" to -1), "kat", generateUnits("kat")),
            UnitAliasGroup("charge", mapOf("A" to 1, "s" to 1), "C", generateUnits("C")),
            UnitAliasGroup("capacitance", mapOf("C" to 1, "V" to -1), "F", generateUnits("F")),
            UnitAliasGroup("flux magnétique", mapOf("V" to 1, "s" to 1), "Wb", generateUnits("Wb")),
            UnitAliasGroup("résistance", mapOf("V" to 1, "A" to -1), "Ohm", generateUnits("Ohm")),
            UnitAliasGroup("conductance", mapOf("Ohm" to -1), "S", generateUnits("S")),
            UnitAliasGroup("activité radioactive", mapOf("s" to -1), "Bq", generateUnits("Bq")),
            UnitAliasGroup("fréquence", mapOf("s" to -1), "Hz", generateUnits("Hz")),
            UnitAliasGroup("dose absorbée", mapOf("J" to 1, "kg" to -1), "Gy", generateUnits("Gy")),
            UnitAliasGroup("dose efficace", mapOf("J" to 1, "kg" to -1), "Sv", generateUnits("Sv")),
            UnitAliasGroup("inductance", mapOf("Wb" to 1, "s" to -1), "H", generateUnits("H")),
            UnitAliasGroup("densité de flux magnétique", mapOf("Wb" to 1, "m" to -2), "T", generateUnits("T")),
            UnitAliasGroup("volume", mapOf("dm" to 3), "L", generateUnits("L")),
        )

        private fun generateUnits(baseUnit: String) = mapOf(
            "p$baseUnit" to 1E-12,
            "n$baseUnit" to 1E-9,
            "µ$baseUnit" to 1E-6,
            "m$baseUnit" to 1E-3,
            "c$baseUnit" to 1E-2,
            "d$baseUnit" to 1E-1,
            "da$baseUnit" to 1E1,
            "h$baseUnit" to 1E2,
            "k$baseUnit" to 1E3,
            "M$baseUnit" to 1E6,
            "G$baseUnit" to 1E9
        )

        fun convert(unit1: PUnit, unit2: PUnit, initialValue: Double): Double? {
            println(initialValue, unit1)
            val initialSignature = unit1.signature
            val targetSignature = unit2.signature
            var overallCoefficient = 1.0

            if (initialSignature == targetSignature) return initialValue
            if (Pair(initialSignature, targetSignature) in convertingCache) {
                return initialValue * convertingCache.getValue(Pair(initialSignature, targetSignature))
            }

            val (flattenedInitialSignature, flatteningCoefficientOfSource) = flattenAliasesOf(initialSignature)
            val (flattenedTargetSignature, flatteningCoefficientOfTarget) = flattenAliasesOf(targetSignature)
            if (flattenedInitialSignature.size != flattenedTargetSignature.size) return null  // Can't succeed

            overallCoefficient *= flatteningCoefficientOfSource / flatteningCoefficientOfTarget

            for ((unit, exponent) in flattenedInitialSignature) {
                val correspondingUnitGroup = unitGroups.firstOrNull { unit in it } ?: throw UnitException("Unit '$unit' wasn't declared.")
                val target = findCompatibleUnitIn(flattenedTargetSignature.keys, unit) ?: return null
                overallCoefficient *= correspondingUnitGroup.getConvertingCoefficient(unit, target)!!.pow(exponent)
            }

            convertingCache[Pair(flattenedInitialSignature, flattenedTargetSignature)] = overallCoefficient
            convertingCache[Pair(flattenedTargetSignature, flattenedInitialSignature)] = 1.0 / overallCoefficient
            convertingCache[Pair(initialSignature, targetSignature)] = overallCoefficient
            convertingCache[Pair(targetSignature, initialSignature)] = 1.0 / overallCoefficient

            return initialValue * overallCoefficient
        }

        private fun findCompatibleUnitIn(availableUnits: Set<String>, target: String): String? {
            val matchingUnits = availableUnits.filter { (unitGroups + aliasesGroups).any { g -> it in g && target in g } }
            if (matchingUnits.size > 1) throw UnitException("Units '${matchingUnits.joinToString(", ")} are ambiguous.")
            return matchingUnits.singleOrNull()
        }

        fun flattenAliasesOf(signature: UnitSignature): Pair<UnitSignature, Double> {
            for ((unit, exponent) in signature) {
                if (aliasesGroups.none { unit in it }) continue

                val aliasGroup = aliasesGroups.single { unit in it }
                val (flattenedAliasSignature, coefficient) = aliasGroup.flattenAlias(unit)!!
                val result = (signature - unit).mergedWith(flattenedAliasSignature.mapValues { (_, v) -> v * exponent }, merge = Int::plus)

                val (flattenedResult, flatteningCoefficient) = flattenAliasesOf(result)
                return flattenedResult to coefficient * flatteningCoefficient
            }
            return signature to 1.0
        }

        fun isConvertible(unit1: PUnit, unit2: PUnit): Boolean {
            return convert(unit1, unit2, 0.0) != null
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

    fun pow(exponent: Double): PUnit {
        if (isNeutral()) return this

        val poweredSignature = signature.mapValues { (_, e) -> e*exponent }
        require(poweredSignature.all { (_, e) -> e.isInt() }) { "Elevating the unit to power $exponent resulted in a unit with non-integer exponents." }
        return PUnit(poweredSignature.mapValues { (_, e) -> e.toInt() })
    }

    fun convert(value: Double, into: PUnit): Double? = convert(this, into, value)
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
