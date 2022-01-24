package physics.knowledge.connections

abstract class DatabaseConnection {
    abstract val cells: Map<String, List<String>>

    // Using lazy { ... } to avoid null leaks
    private val columnNames by lazy { cells.keys }
    val labeledLines by lazy { List(cells.values.first().size) { index -> getLabeledLine(index) } }

    private fun getLine(index: Int): List<String> {
        return cells.values.map { it[index] }
    }

    private fun getLabeledLine(index: Int): Map<String, String> {
        return columnNames.zip(getLine(index)).toMap()
    }

    fun getCell(column: String, line: Int): String {
        return cells.getValue(column)[line]
    }
}
