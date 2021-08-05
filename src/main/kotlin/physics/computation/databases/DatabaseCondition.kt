package physics.computation.databases

sealed class DatabaseCondition {
    abstract infix fun matches(columnValues: Map<String, String>): Boolean

    class Equal(private val column: String, private val expected: String) : DatabaseCondition() {
        override fun matches(columnValues: Map<String, String>): Boolean {
            return columnValues.getValue(column) == expected
        }
    }
}