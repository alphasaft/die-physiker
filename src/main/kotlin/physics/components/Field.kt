package physics.components

import physics.FieldHasUnknownValueException
import physics.computation.PhysicalKnowledge
import physics.values.PhysicalValue
import kotlin.reflect.KClass


class Field<T : PhysicalValue<*>> private constructor(
    val name: String,
    val factory: PhysicalValue.Factory<T>,
    initialContent: T? = null
) {
    private var _content: T? = initialContent
    private var _obtainedBy: PhysicalKnowledge? = null ; val obtainedBy get() = _obtainedBy
    val type get() = factory.of

    fun isKnown() = _content != null

    fun getContentOrNull() = _content
    fun getContent() = _content ?: throw FieldHasUnknownValueException(this.name)

    fun setContent(value: T, obtentionMethod: PhysicalKnowledge) {
        _content = value
        _obtainedBy = obtentionMethod
    }

    override fun toString(): String {
        return when {
            _content == null -> "$name (inconnu(e))"
            _obtainedBy == null -> "$name = $_content"
            else -> "$name = $_content (obtenu(e) par $obtainedBy)"
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

    class Template<T : PhysicalValue<*>>(
        val name: String,
        private val factory: PhysicalValue.Factory<T>,
    ) {
        fun newField(value: String? = null): Field<*> {
            val computedValue = value?.let { factory.fromString(it) }
            return Field(name, factory, computedValue)
        }
    }
}
