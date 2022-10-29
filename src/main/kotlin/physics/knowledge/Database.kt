package physics.knowledge

import normalize
import physics.InappropriateKnowledgeException
import physics.components.ComponentClass
import physics.components.Field
import physics.components.Context
import physics.knowledge.connections.DatabaseConnection
import physics.knowledge.connections.FileDatabaseConnection
import physics.quantities.PValue
import physics.quantities.PString


class Database(
    override val name: String,
    flags: Int,
    private val connection: DatabaseConnection,
    given: ComponentClass,
    thenLink: Map<String, String>? = null
) : Knowledge {

    object Flags {
        const val CASE_INSENSITIVE = 1
        const val NORMALIZE = 2
    }

    constructor(
        name: String,
        options: Int,
        from: String,
        given: ComponentClass,
        thenLink: Map<String, String>?
    ) : this(name, options, FileDatabaseConnection(from), given, thenLink)

    private val componentClass = given
    private val fieldsLinkedToColumns = thenLink ?: componentClass.structure.fieldsNames.associateWith { it }
    private val caseInsensitive = flags and Flags.CASE_INSENSITIVE != 0
    private val normalize = flags and Flags.NORMALIZE != 0

    private fun applyOptions(string: String): String {
        var s = string
        if (caseInsensitive) s = s.lowercase()
        if (normalize) s = s.normalize()
        return s
    }

    override fun <T : PValue<T>> getFieldValue(
        field: Field<T>,
        context: Context
    ): T {
        fun error(message: String? = null): Nothing = throw InappropriateKnowledgeException(this, field.name, message)

        val fieldOwner = context.findFieldOwner(field)
        if (fieldOwner notInstanceOf componentClass) error()
        if (field.name !in fieldsLinkedToColumns.keys) error()

        val knownFields = fieldsLinkedToColumns.keys.filter { fieldOwner.getQuantity(it) is PValue<*> }
        if (knownFields.isEmpty()) error("No field has a known value in order to perform the query.")

        val chosenField = knownFields.first()
        val queryResult = PString(connection.getCell(
            fieldsLinkedToColumns.getValue(field.name),
            connection.labeledLines.indexOf(connection.labeledLines.firstOrNull {
                applyOptions(fieldOwner.getQuantity(chosenField).toString()) ==
                        applyOptions(it.getValue(fieldsLinkedToColumns.getValue(chosenField)))
            } ?: error(
                "No line was found in table '${this.name}' with '${
                    fieldsLinkedToColumns.getValue(
                        chosenField
                    )
                }' taking the value '${
                    fieldOwner.getQuantity(chosenField)
                }'."
            )))).toPValue(field.type)

        return queryResult
    }

    override fun toString(): String {
        return name
    }
}
