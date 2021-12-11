package physics.computation.connections

class DatabaseDummyConnection(
    columns: List<String>,
    lines: List<List<String>>,
) : DatabaseConnection() {
    override val cells: Map<String, List<String>> = columns.mapIndexed { i, c -> c to lines.map { l -> l[i] } }.toMap()
}
