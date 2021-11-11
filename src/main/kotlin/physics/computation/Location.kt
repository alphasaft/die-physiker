package physics.computation


sealed class Location {
    object Any : Location()

    data class At(val alias: String, val field: String) : Location() {
        constructor(formattedLocation: String) : this(
            formattedLocation.split(".").first(),
            formattedLocation.split(".").last()
        )
    }
}
