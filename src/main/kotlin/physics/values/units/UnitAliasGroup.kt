package physics.values.units

class UnitAliasGroup(
    measuredQuantity: String,
    private val mainAlias: String,
    private val signature: UnitSignature,
) : UnitGroup(measuredQuantity, mainAlias) {

    fun flattenAlias(alias: String): Pair<UnitSignature, Double>? {
        val coefficient = convert(alias, mainAlias, 1.0) ?: return null
        return signature to coefficient
    }
}