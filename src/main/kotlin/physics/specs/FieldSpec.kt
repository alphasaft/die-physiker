package physics.specs

import physics.builtinNamesToClasses
import kotlin.reflect.KClass

data class FieldSpec(
    val name: String,
    val type: KClass<*>,
) {
    constructor(name: String, builtinType: String): this(name, builtinNamesToClasses.getValue(builtinType))
}
