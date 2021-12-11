import physics.values.units.StdUnitScope


enum class ScientificNotationDisplayStyle {
    E,
    POWER_OF_TEN,
}

enum class FieldComputationMethod {
    ACCURATE,
    LAZY,
}

enum class CheckCoherenceOfFields {
    YES,
    NO
}


object Settings {
    var scientificNotationDisplayStyle: ScientificNotationDisplayStyle = ScientificNotationDisplayStyle.POWER_OF_TEN
    var fieldComputationMethod: FieldComputationMethod = FieldComputationMethod.ACCURATE
    var checkCoherenceOfFields: CheckCoherenceOfFields = CheckCoherenceOfFields.NO
}
