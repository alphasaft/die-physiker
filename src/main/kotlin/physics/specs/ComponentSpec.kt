package physics.specs


data class ComponentSpec(
    val name: String,
    val type: String,
    val atLeast: Int = 0,
    val atMost: Int = -1
) {
    constructor(name: String, type: String, exactly: Int): this(name, type, atLeast = exactly, atMost = exactly)
}