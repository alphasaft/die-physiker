package physics.values.units

import mergedWith
import physics.AmbiguousUnitException
import physics.ConversionNeededException
import physics.IncompatibleUnitsException
import physics.values.toInt


class PhysicalUnit(private val scope: UnitScope, internal val signature: UnitSignature) {

    constructor(scope: UnitScope, symbols: String): this(scope, kotlin.run {
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

    init {
        if (signature.keys.any { unit1 -> (signature.keys - unit1).any { unit2 -> scope.isConvertible(PhysicalUnit(scope, unit1), PhysicalUnit(scope, unit2)) } }) {
            throw AmbiguousUnitException(this)
        }
    }

    fun isNeutral() = scope == NeutralUnitScope
    fun getScope() = scope

    operator fun times(other: PhysicalUnit): PhysicalUnit {
        return when {
            this.isNeutral() -> other
            other.isNeutral() -> this
            other != this && scope.isConvertible(this, other) -> throw ConversionNeededException(other, this)
            else -> PhysicalUnit(scope, signature
                .mergedWith(other.signature, merge = Int::plus)
                .filterValues { it != 0 })
        }
    }

    operator fun div(other: PhysicalUnit): PhysicalUnit {
        if (other != this && scope.isConvertible(this, other)) throw ConversionNeededException(other, this)

        return PhysicalUnit(scope, signature
            .mergedWith(other.signature.mapValues { (_, exponent) -> -exponent }, merge = Int::plus)
            .filterValues { it != 0 })
    }

    fun invert(): PhysicalUnit {
        return PhysicalUnit(scope, signature.mapValues { (_, v) -> -v })
    }

    operator fun plus(other: PhysicalUnit): PhysicalUnit {
        when {
            signature == other.signature || isNeutral() || other.isNeutral() -> return this
            scope.isConvertible(this, other) -> throw ConversionNeededException(other, this)
            else -> throw IncompatibleUnitsException(this, other)
        }
    }

    operator fun minus(other: PhysicalUnit): PhysicalUnit = this + other

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
}
