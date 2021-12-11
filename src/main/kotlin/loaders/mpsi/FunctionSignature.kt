package loaders.mpsi

internal class FunctionSignature(val parameters: Map<String, MpsiType>, private val returnType: MpsiType) {
    override fun equals(other: Any?): Boolean {
        return other is FunctionSignature && other.parameters == parameters && other.returnType == returnType
    }

    override fun toString(): String {
        return "(${parameters.toList().joinToString(", ") { (p, t) -> "$p: $t" }}): $returnType"
    }

    override fun hashCode(): Int {
        var result = parameters.hashCode()
        result = 31 * result + returnType.hashCode()
        return result
    }
}