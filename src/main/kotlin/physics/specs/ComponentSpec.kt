package physics.specs


data class ComponentSpec(
    val location: FieldAccessSpec,
    val name: String,
    val select: Char = '?',
) {
    constructor(formattedLocation: String, name: String, select: Char = '?'): this(FieldAccessSpec(formattedLocation), name, select)

    val parentName get() = location.fieldOwner
    val storedInto get() = location.fieldName
}
