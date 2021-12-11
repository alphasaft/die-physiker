package loaders.mpsi.statements

import loaders.mpsi.FunctionSignature

internal class ExternalFunctionDeclaration(val functionName: String, val signature: FunctionSignature) : ExternalDeclaration {
    override fun toString(): String {
        return "expect function $functionName$signature"
    }
}
