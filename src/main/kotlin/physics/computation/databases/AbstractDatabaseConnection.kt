package physics.computation.databases

abstract class AbstractDatabaseConnection : DatabaseConnection {
    abstract val columns: Map<String, List<String>>
    private val lines by lazy { List(columns.values.first().size) { i -> getLine(i) } }  // Avoid null leaks

    private fun getLine(index: Int): List<String> {
        return columns.values.map { it[index] }
    }

    override fun select(what: String, where: Map<String, String>): String {
        for (line in lines) {
            if (where.all { (column, expected) -> columns.keys.zip(line).toMap().getValue(column) == expected }) {
                return what
            }
        }
        throw IllegalArgumentException()
    }
}
