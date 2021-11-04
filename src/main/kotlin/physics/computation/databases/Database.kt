package physics.computation.databases

import physics.EmptyQueryResult
import physics.InappropriateKnowledgeException
import physics.components.ComponentClass
import physics.components.Field
import physics.components.PhysicalSystem
import physics.computation.PhysicalKnowledge
import physics.computation.databases.connections.DatabaseConnection
import physics.computation.databases.connections.FileStoredDatabase
import physics.values.PhysicalValue


// TODO : Improve error gestion in the `Database` and the `Formula` class

class Database(
    val name: String,
    private val connection: DatabaseConnection,
    given: ComponentClass,
    thenLink: Map<String, String>? = null
) : PhysicalKnowledge {

    constructor(
        name: String,
        from: String,
        given: ComponentClass,
        thenLink: Map<String, String>?
    ): this(name, FileStoredDatabase(from), given, thenLink)

    private val componentClass = given
    private val fieldsLinkedToColumns = thenLink ?: componentClass.fields.associateWith { it }

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
                    fieldOwner.get<PhysicalValue<*>>(chosenField).toString() ==
                            it.getValue(fieldsLinkedToColumns.getValue(chosenField))
            } ?: throw EmptyQueryResult(
                column = fieldsLinkedToColumns.getValue(chosenField),
                value = fieldOwner[chosenField]
            )
        ))) to this
    }

    override fun toString(): String {
        return "Database '$name'"
    }
}
