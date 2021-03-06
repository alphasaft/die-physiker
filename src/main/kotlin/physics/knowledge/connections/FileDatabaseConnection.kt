package physics.knowledge.connections

import java.io.File


class FileDatabaseConnection(
    fileName: String,
    separator: String = ","
) : DatabaseConnection() {
    override val cells: Map<String, List<String>> = run {
        val linesAndHeader = File(System.getProperty("user.dir") + "\\src\\main\\resources\\" + fileName)
            .readLines()
            .filterNot { it.isBlank() }
            .map { it.split(Regex("(?!\\\\)$separator")) }

        val columnNames = linesAndHeader.first().map { it.trim() }
        val lines = linesAndHeader.drop(1)

        return@run columnNames.mapIndexed { index, column -> column to lines.map { it[index].trim() } }.toMap()
    }
}