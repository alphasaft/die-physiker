package physics.specs

import physics.ComponentVariableName
import physics.FieldName

data class FieldAccessSpec(
    val fieldOwner: ComponentVariableName,
    val fieldName: FieldName
) {
    constructor(formattedSpec: String): this(
        formattedSpec.split(".").first(),
        formattedSpec.split(".").last()
    )
}
