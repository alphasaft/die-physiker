package physics.values

object PhysicalDoubleTests {
    fun runAll() {
        ensureParsingWorks()
        ensureRoundingIsDoneCorrectly()
        ensureOperationsWork()
    }

    private fun assertEqual(actual: PhysicalDouble, expected: Double) {
        require(actual.value == expected) { "Expected $expected, got ${actual.value}." }
    }

    private fun assertEqual(actual: Any, expected: Any) {
        require(actual == expected) { "Expected $expected, got $actual." }
    }

    private fun ensureParsingWorks() {
        assertEqual(PhysicalDouble("4.2 * 10^-1").significantDigitsCount, 2)
        assertEqual(PhysicalDouble("7.22 * 10^1").significantDigitsCount, 3)
        assertEqual(PhysicalDouble("3.4 * 10^3"), 3400.0)
        assertEqual(PhysicalDouble("8E-1"), 0.8)
    }

    private fun ensureRoundingIsDoneCorrectly() {
        assertEqual(PhysicalDouble(0.002, significantDigitsCount = 2), 0.002)
        assertEqual(PhysicalDouble(0.034, significantDigitsCount = 1), 0.03)
        assertEqual(PhysicalDouble(0.0656, significantDigitsCount = 2), 0.066)
    }

    private fun ensureOperationsWork() {
        assertEqual(PhysicalDouble("33") + PhysicalDouble("4.5"), 38.0)
        assertEqual(PhysicalDouble("67") * PhysicalDouble("3.33"), 220.0)
    }

}