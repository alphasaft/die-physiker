package loaders.mpsi.statements

import physics.quantities.PString
import physics.quantities.PValue

class MpsiBuiltinLiteral(val value: PValue<*>) : Expression() {
    override fun toString(): String {
        return if (value is PString) "\"$value\""
        else value.toString()
    }
}
