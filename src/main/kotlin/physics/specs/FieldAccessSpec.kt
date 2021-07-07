package physics.specs

import physics.ComponentVariableName


data class FieldAccessSpec(
    val fieldOwner: ComponentVariableName,
    val fieldName: String
) {
    constructor(formattedSpec: String): this(
        formattedSpec.split(".").first(),
        formattedSpec.split(".").last()
    )
}
