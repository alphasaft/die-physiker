package physics.computation.databases

class RuntimeDatabaseConnection(
    columns: List<String>,
    lines: List<List<String>>,
) : AbstractDatabaseConnection() {
    override val columns: Map<String, List<String>> = columns.mapIndexed { i, c -> c to lines.map { l -> l[i] } }.toMap()
}