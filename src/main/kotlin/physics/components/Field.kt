package physics.components

import Mapper
import assert
import noop
import physics.quantities.AnyQuantity
import physics.quantities.ImpossibleQuantity
import physics.quantities.PValue
import physics.quantities.Quantity
import kotlin.reflect.KClass


class Field<T : PValue<T>> private constructor(
    @Deprecated("When representing field, should use 'representation'") val name: String,
    val type: KClass<T>,
    val representation: String,
    private var content: Quantity<T>,
    private val contentNormalizer: Mapper<Quantity<T>>,
) {
    fun getContent() = content

    fun setContent(content: Quantity<*>) {
        require(type == content.type) { "Expected quantity of type ${type.simpleName}, got ${content.type.simpleName}." }

        val newContent = this.content simpleIntersect contentNormalizer(content.assert<Quantity<T>>())
        require(newContent !is ImpossibleQuantity<*>) { "Can't set content of field $representation to $content, since it isn't compatible with the current content (${this.content})." }

        this.content = newContent
    }

    override fun toString(): String {
        return "$representation : $content"
    }

    override fun equals(other: Any?): Boolean {
        return other is Field<*> && other.type == type && other.content == content && other.name == name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }

    class Template<T : PValue<T>>(
        val type: KClass<T>,
        val name: String,
        private val notation: Notation,
        private val initialQuantity: Quantity<T> = AnyQuantity(type),
        private val normalizer: (Quantity<T>) -> Quantity<T> = ::noop
    ) {
        @Suppress("unused")
        sealed class Notation(val default: String, val custom: (String) -> String) {
            class Always(notation: String) : Notation(notation, { notation })
            class UseNoEmbedding(notation: String) : Notation(notation, { "$notation$it" })
            class UseParenthesis(notation: String) : Notation(notation, { "$notation($it)" })
            class UseBrackets(notation: String) : Notation(notation, { "$notation[$it]" })
            class UseLtAndGt(notation: String) : Notation(notation, { "$notation<$it>" })
            class UseUnderscore(notation: String) : Notation(notation, { "${notation}_$it" })
            class Custom(default: String, custom: (String) -> String) : Notation(default, custom)
        }

        companion object Factory {
            inline operator fun <reified T : PValue<T>> invoke(
                name: String,
                notation: Notation,
                initialQuantity: Quantity<T> = AnyQuantity(),
                noinline normalizer: (Quantity<T>) -> Quantity<T> = ::noop
            ) = Template(
                T::class,
                name,
                notation,
                initialQuantity,
                normalizer
            )
        }

        internal fun newField(quantity: Quantity<*> = AnyQuantity(type), ownerName: String? = null): Field<T> {
            val representation = if (ownerName == null) notation.default else notation.custom(ownerName)
            return Field(name, type, representation, initialQuantity, normalizer).apply { setContent(quantity) }
        }
    }
}
