package physics.quantities.units

class AliasedUnitGroup(
    measuredQuantity: String,
    private val signature: UnitSignature,
    private val mainAlias: String,
    secondaryAliases: Map<String, Double>,
) : UnitGroup(measuredQuantity, mainAlias, secondaryAliases) {

    fun flattenAlias(alias: String): Pair<UnitSignature, Double>? {
        val coefficient = convert(alias, mainAlias, 1.0) ?: return null
        return signature to coefficient
    }
}
