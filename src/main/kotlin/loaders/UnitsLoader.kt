package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.values.units.*

object UnitsLoader : DataLoader<UnitsParser, UnitScope>(UnitsParser) {
    private val unitPrefixes = mapOf(
        "n" to 10E-9,
        "µ" to 10E-6,
        "m" to 10E-3,
        "c" to 10E-2,
        "d" to 10E-1,
        "da" to 10E1,
        "h" to 10E2,
        "k" to 10E3,
        "M" to 10E6,
        "G" to 10E9
    )

    override fun generateFrom(ast: Ast): UnitScope {
        val scope = StdUnitScope()
        val allUnitGroups = ast.allNodes("unitGroup-#").map { generateGroupFrom(it) }
        val aliases = allUnitGroups.filterIsInstance<UnitAliasGroup>()
        val standardUnitGroups = allUnitGroups.filterIsInstance<StandardUnitGroup>()
        for (aliasGroup in aliases) scope.addAliasGroup(aliasGroup)
        for (standardUnitGroup in standardUnitGroups) scope.addGroup(standardUnitGroup)
        return scope
    }
    
    private fun generateGroupFrom(groupNode: AstNode): UnitGroup {
        val measuredQuantity = groupNode["measuredQuantity"]
        val (mainUnitSymbol, associatedSignature) = generateMainUnitFrom(groupNode.."mainUnit")
        val otherUnits = generateOtherUnitsFrom(groupNode.."units", mainUnitSymbol)
        val group = if (associatedSignature == null) StandardUnitGroup(measuredQuantity, mainUnitSymbol)
        else UnitAliasGroup(measuredQuantity, mainUnitSymbol, associatedSignature)
        for ((unit, convertingCoefficient) in otherUnits) group.addUnit(unit, convertingCoefficient)
        return group
    }

    private fun generateMainUnitFrom(mainUnitNode: AstNode): Pair<String, UnitSignature?> {
        val symbol = mainUnitNode["symbol"]
        val associatedSignature = mainUnitNode.getNodeOrNull("associatedSignature")?.let { generateUnitSignatureFrom(it) }
        return symbol to associatedSignature
    }

    private fun generateUnitSignatureFrom(associatedSignatureNode: AstNode): UnitSignature {
        val result = mutableMapOf<String, Int>()
        for (signaturePart in associatedSignatureNode.allNodes("signaturePart-#")) {
            result[signaturePart["unit"]] = signaturePart["exponent"].toInt()
        }
        return result
    }

    private fun prefixUnit(mainUnitSymbol: String): Map<String, Double> {
        return unitPrefixes.mapKeys { (prefix, _) -> prefix + mainUnitSymbol }
    }

    private fun generateOtherUnitsFrom(unitsNode: AstNode, mainUnitSymbol: String): Map<String, Double> {
        val generatedUnits = mutableMapOf(mainUnitSymbol to 1.0)
        for (unitNode in unitsNode.allNodes("unit-#")) {
            generatedUnits += generateUnitFrom(unitNode, generatedUnits)
        }
        return generatedUnits
    }

    private fun generateUnitFrom(unitNode: AstNode, generatedUnits: Map<String, Double>): Pair<String, Double> {
        val unitSymbol = unitNode["symbol"]
        val converter = unitNode.."converter"
        val convertedInMainUnit = converter["coefficient"].toDouble() * generatedUnits.getValue(converter["target"])
        return unitSymbol to convertedInMainUnit
    }
}