package loaders

import loaders.base.Ast
import loaders.base.DataLoader
import physics.values.units.UnitSignature

class UnitSignatureLoader : DataLoader<UnitSignatureParser, UnitSignature>(UnitSignatureParser) {
    override fun generateFrom(ast: Ast): UnitSignature {
        val result = mutableMapOf<String, Int>()
        for (unitPartNode in ast.allNodes("signaturePart-#")) {
            result[unitPartNode["unit"]] = unitPartNode["exponent"].toInt()
        }
        return result
    }
}