package physics.values.units

import Couple
import mergeWith
import physics.AmbiguousUnitException
import physics.UnknownUnitException
import println
import kotlin.math.pow


internal class StdUnitScope : UnitScope() {
    private val convertingCache = mutableMapOf<Couple<UnitSignature>, Double>()
    private val unitGroups = mutableListOf<StandardUnitGroup>()
    private val aliasesGroups = mutableListOf<UnitAliasGroup>()

    fun addGroup(unitGroup: StandardUnitGroup) {
        unitGroups.add(unitGroup)
    }

    fun addAliasGroup(unitAliasGroup: UnitAliasGroup) {
        aliasesGroups.add(unitAliasGroup)
    }

    override fun convertImpl(unit1: PhysicalUnit, unit2: PhysicalUnit, initialValue: Double): Double? {
        val initialSignature = unit1.signature
        val targetSignature = unit2.signature

        if (initialSignature == targetSignature || unit1.isNeutral() || unit2.isNeutral()) return initialValue
        if (Pair(initialSignature, targetSignature) in convertingCache) return convertingCache.getValue(
            Pair(
                initialSignature,
                targetSignature
            )
        ) * initialValue

        var value = initialValue
        val (flattenedInitialSignature, flatteningCoefficientOfSource) = flattenAliasesOf(initialSignature)
        val (flattenedTargetSignature, flatteningCoefficientOfTarget) = flattenAliasesOf(targetSignature)
        if (flattenedInitialSignature.size != flattenedTargetSignature.size) return null  // Can't succeed

        value *= flatteningCoefficientOfSource / flatteningCoefficientOfTarget

        for ((unit, exponent) in flattenedInitialSignature) {
            val correspondingUnitGroup = unitGroups.firstOrNull { unit in it } ?: throw UnknownUnitException(PhysicalUnit(this, unit))
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
        if (matchingUnits.size > 1) throw AmbiguousUnitException(PhysicalUnit(this, target))
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

    // this is actually the song that finally kicked me in the ass and made me finally write my essay on rapture and columbia for wich i got an a. thanks guys

}
