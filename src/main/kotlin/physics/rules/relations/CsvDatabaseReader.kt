package physics.rules.relations

import java.io.File


class CsvDatabaseReader(file: File) : BaseDatabaseReader(parseCsvFile(file)) {
    constructor(fileName: String): this(File(fileName))

    private companion object Parser {
        fun parseCsvFile(file: File): List<Map<String, String>> {
            val result = mutableListOf<Map<String, String>>()

            val lines = file.readLines()
            val columnNames = lines[0].split(",").map(String::trim)
            for (line in lines.drop(1)) {
                val cells = line.split(",").map(String::trim)
                result.add(columnNames.zip(cells).toMap())
            }

            return result
        }
    }
}