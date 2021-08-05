package physics.computation.databases

import java.io.File


class FileStoredDatabase(
    fileName: String,
) : AbstractDatabaseConnection() {
    override val columns: Map<String, List<String>> = run {
        val linesAndHeader = File(fileName).readLines().map { it.split(";") }
        val columnNames = linesAndHeader.first()
        val lines = linesAndHeader.drop(1)

        return@run columnNames.mapIndexed { index, column -> column to lines.map { it[index] } }.toMap()
    }
}