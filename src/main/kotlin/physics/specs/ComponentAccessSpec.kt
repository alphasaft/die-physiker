package physics.specs


data class ComponentAccessSpec(
    val location: FieldAccessSpec,
    val name: String,
) {
    constructor(formattedLocation: String, name: String): this(FieldAccessSpec(formattedLocation), name)

    val selectAll get() = name.endsWith("#")
    val parentName get() = location.fieldOwner
    val storedInto get() = location.fieldName
}
