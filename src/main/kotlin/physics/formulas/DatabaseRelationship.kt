package physics.formulas

import physics.components.Field
import physics.components.PhysicalSystem
import physics.formulas.databases.DatabaseCondition
import physics.formulas.databases.DatabaseConnection
import physics.values.PhysicalValue
import java.io.File

class DatabaseRelationship(
    fileName: String,
    val loader: DatabaseConnection,
) : PhysicalRelationship {

    private val fileContent = File(fileName).readText()
    private val name: String = fileName.split(".").dropLast(1).joinToString(" ")

    override fun <T : PhysicalValue<*>> computeFieldValue(
        field: Field<T>,
        system: PhysicalSystem
    ): Pair<T, PhysicalRelationship> {
        return
    }
}