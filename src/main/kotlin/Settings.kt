enum class ScientificNotationDisplayStyle {
    E,
    POWER_OF_TEN,
}

enum class FieldComputationMethod {
    ACCURATE,
    LAZY,
}


object Settings {
    var scientificNotationDisplayStyle: ScientificNotationDisplayStyle = ScientificNotationDisplayStyle.POWER_OF_TEN
    var fieldComputationMethod: FieldComputationMethod = FieldComputationMethod.ACCURATE
}