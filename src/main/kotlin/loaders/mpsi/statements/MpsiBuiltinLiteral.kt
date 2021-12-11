package loaders.mpsi.statements

import physics.values.PhysicalString
import physics.values.PhysicalValue

class MpsiBuiltinLiteral(val value: PhysicalValue<*>) : Expression() {
    override fun toString(): String {
        return if (value is PhysicalString) "\"$value\""
        else value.toString()
    }
}
