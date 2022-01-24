package physics.components

import noop
import physics.quantities.AnyQuantity
import physics.quantities.ImpossibleQuantity
import physics.quantities.PValue
import physics.quantities.Quantity
import kotlin.reflect.KClass


class Field<T : PValue<T>> private constructor(
    val name: String,
    val type: KClass<T>,
    private val notation: Notation,
    private var content: Quantity<T>,
    private val contentNormalizer: (Quantity<T>) -> Quantity<T>,
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

    lateinit var owner: Component

    fun getContent() = content

    fun setContent(content: Quantity<*>) {
        require(type == content.type) { "Expected quantity of type ${type.simpleName}, got ${content.type.simpleName}." }

        val newContent = this.content intersect contentNormalizer(@Suppress("UNCHECKED_CAST") (content as Quantity<T>))
        require(newContent !is ImpossibleQuantity<*>) { "Can't set field's content to $content, since it isn't compatible with the current content." }

        this.content = newContent
    }

    fun getNotation() =
        if (owner.isCustomRepresentationAvailable()) notation.custom(owner.toString())
        else notation.default

    private fun toStringUsingNotation(notation: String): String =
        if (content is AnyQuantity<*>) "$notation (inconnu(e))" else "$notation : $content"

    override fun toString(): String =
        toStringUsingNotation(getNotation())

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
        private val initialQuantity: Quantity<T>,
        private val normalizer: (Quantity<T>) -> Quantity<T>
    ) {
        companion object Factory {
            inline operator fun <reified T : PValue<T>> invoke(
                name: String,
                notation: Notation,
                initialQuantity: Quantity<T>,
                noinline normalizer: (Quantity<T>) -> Quantity<T>
            ) = Template(
                T::class,
                name,
                notation,
                initialQuantity,
                normalizer
            )

            inline operator fun <reified T : PValue<T>> invoke(
                name: String,
                notation: String,
                initialQuantity: Quantity<T>,
                noinline normalizer: (Quantity<T>) -> Quantity<T>
            ) = this(
                name,
                Notation.UseParenthesis(notation),
                initialQuantity,
                normalizer
            )

            inline operator fun <reified T : PValue<T>> invoke(
                name: String,
                notation: String,
                initialQuantity: Quantity<T>,
            ) = this(
                name,
                notation,
                initialQuantity,
                ::noop
            )

            inline operator fun <reified T : PValue<T>> invoke(
                name: String,
                notation: String,
            ) = this(
                name,
                notation,
                AnyQuantity<T>(),
                ::noop
            )
        }

        internal fun newField(quantity: Quantity<*> = AnyQuantity(type)): Field<T> {
            return Field(name, type, notation, initialQuantity, normalizer).apply { setContent(quantity) }
        }
    }
}
