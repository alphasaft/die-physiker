package loaders.mpsi.statements

import loaders.mpsi.MpsiType


internal data class ExternalVariableDeclaration(
    val varName: String,
    val varType: MpsiType,
    val loadedFrom: String?,
) : ExternalDeclaration {
    override fun toString(): String {
        return if (loadedFrom == null) "expect var $varName: $varType"
        else "expect var $varName: $varType from $loadedFrom"
    }
}
