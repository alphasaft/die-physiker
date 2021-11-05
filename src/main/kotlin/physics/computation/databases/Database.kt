package physics.computation.databases

import normalize
import physics.EmptyQueryResult
import physics.InappropriateKnowledgeException
import physics.components.ComponentClass
import physics.components.Field
import physics.components.PhysicalSystem
import physics.computation.PhysicalKnowledge
import physics.computation.databases.connections.DatabaseConnection
import physics.computation.databases.connections.FileDatabaseConnection
import physics.values.PhysicalValue


class Database(
    override val name: String,
    private val options: Int,
    private val connection: DatabaseConnection,
    given: ComponentClass,
    thenLink: Map<String, String>? = null
) : PhysicalKnowledge {

    constructor(
        name: String,
        options: Int,
        from: String,
        given: ComponentClass,
        thenLink: Map<String, String>?
    ): this(name, options, FileDatabaseConnection(from), given, thenLink)

    private val componentClass = given
    private val fieldsLinkedToColumns = thenLink ?: componentClass.fields.associateWith { it }
    private val caseInsensitive = options and DatabaseOptions.CASE_INSENSITIVE != 0
    private val normalize = options and DatabaseOptions.NORMALIZE != 0

    private fun applyOptions(string: String): String {
        var s = string
        if (caseInsensitive) s = s.lowercase()
        if (normalize) s = s.normalize()
        return s
    }

    override fun <T : PhysicalValue<*>> getFieldValue(
        field: Field<T>,
        system: PhysicalSystem
    ): Pair<T, PhysicalKnowledge> {
        if (field.name !in fieldsLinkedToColumns.keys) throw InappropriateKnowledgeException(this, field.name)

        val fieldOwner = system.findFieldOwner(field)
        if (fieldOwner notInstanceOf componentClass) throw InappropriateKnowledgeException(this, field.name)

        val knownFields = fieldsLinkedToColumns.keys.filter { fieldOwner.getOrNull<PhysicalValue<*>>(it) != null }
        if (knownFields.isEmpty()) throw InappropriateKnowledgeException(this, field.name, "No field has a known value in order to perform the query.")
        val chosenField = knownFields.first()

        return field.factory.fromString(connection.getCell(
            fieldsLinkedToColumns.getValue(field.name),
            connection.labeledLines.indexOf(connection.labeledLines.firstOrNull {
                    applyOptions(fieldOwner.get<PhysicalValue<*>>(chosenField).toString()) ==
                    applyOptions(it.getValue(fieldsLinkedToColumns.getValue(chosenField)))
            } ?: throw EmptyQueryResult(
                table = this.name,
                column = fieldsLinkedToColumns.getValue(chosenField),
                value = fieldOwner[chosenField]
            )
        ))) to this
    }

    override fun toString(): String {
        return "Database '$name'"
    }
}
