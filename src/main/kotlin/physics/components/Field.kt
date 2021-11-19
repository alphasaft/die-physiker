package physics.components

import physics.FieldHasUnknownValueException
import physics.computation.PhysicalKnowledge
import physics.values.PhysicalValue


class Field<T : PhysicalValue<*>> private constructor(
    val name: String,
    private val adaptableNotation: String?,
    private val defaultNotation: String,
    val factory: PhysicalValue.Factory<T>,
    initialContent: T? = null
) {

    private var _content: T? = initialContent
    private var _obtainedBy: PhysicalKnowledge? = null ; val obtainedBy get() = _obtainedBy
    private var _obtentionMethodSpecificRepresentation: String? = null ; val obtentionMethodSpecificRepresentation get() = _obtentionMethodSpecificRepresentation
    val type get() = factory.of

    fun isKnown() = _content != null

    fun getContentOrNull() = _content
    fun getContent() = _content ?: throw FieldHasUnknownValueException(this.name)

    fun setContent(value: T, obtentionMethod: PhysicalKnowledge?, obtentionMethodRepresentation: String?) {
        _content = value
        _obtainedBy = obtentionMethod
        _obtentionMethodSpecificRepresentation = obtentionMethodRepresentation
    }

    override fun toString(): String {
        return toStringUsingNotation(defaultNotation)
    }

    fun toString(owner: Component): String {
        return toStringUsingNotation(getNotationFor(owner))
    }

    fun getNotationFor(owner: Component): String {
        val ownerCustomRepresentation = owner.toStringCustom()
        return if (ownerCustomRepresentation == null || adaptableNotation == null) defaultNotation
        else adaptableNotation.replace("?", ownerCustomRepresentation)
    }

    private fun toStringUsingNotation(notation: String): String {
        return when {
            _content == null -> "$notation (inconnu(e))"
            _obtentionMethodSpecificRepresentation == null -> "$notation = $_content"
            else -> "$notation = $_content (obtenu(e) par $obtentionMethodSpecificRepresentation)"
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is Field<*> && other.type == type && other._content == _content && other.name == name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (_content?.hashCode() ?: 0)
        result = 31 * result + (_obtainedBy?.hashCode() ?: 0)
        return result
    }

    fun copy() = Field(name, adaptableNotation, defaultNotation, factory, getContentOrNull())

    class Template<T : PhysicalValue<*>>(
        val name: String,
        notation: String,
        private val factory: PhysicalValue.Factory<T>,
    ) {
        constructor(
            name: String,
            factory: PhysicalValue.Factory<T>
        ): this(name, name, factory)

        init {
            if (notation.count { it == '|' } > 1) throw IllegalArgumentException("Expected at most one '|' in the notation")
        }

        private val adaptableNotation = if ("|" in notation) notation.split("|").first().trim() else null
        private val defaultNotation = notation.split("|").last().trim()

        fun newField(assignment: String?): Field<*> {
            if (assignment == null) return Field(name, adaptableNotation, defaultNotation, factory)

            val (chosenNotation, value) = extractNotationAndValueFrom(assignment)
            val computedValue = factory.fromString(value)
            return if (chosenNotation == null) Field(name, adaptableNotation, defaultNotation, factory, computedValue)
            else Field(name, null, chosenNotation, factory, computedValue)
        }

        private fun extractNotationAndValueFrom(statement: String): Pair<String?, String> {
            if ("=" !in statement) return null to statement
            val (notation, value) = statement.split("=").map { it.trim() }
            return notation to value
        }
    }
}
