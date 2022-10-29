package physics.components

import Mapper
import noop
import physics.components.history.HistoryOwner
import physics.quantities.*
import kotlin.reflect.KClass

class Field<T : PValue<T>> private constructor(
    @Deprecated("When representing field, should use 'representation'") val name: String,
    val type: KClass<T>,
    val representation: String,
    private var content: Quantity<T>,
    private val contentNormalizer: Mapper<Quantity<T>>,
) : HistoryOwner() {

    init {
        if (content !is AnyQuantity)
            tell("Initialized with value $content.")
    }

    override fun asHeader(): String = representation

    fun getContent() = content

    fun setContent(content: Quantity<*>) {
        val convertedContent = content.toQuantity(type)
        val newContent = this.content simpleIntersect contentNormalizer(convertedContent)
        require(newContent !is ImpossibleQuantity<*>) { "Crash : $representation = $content : not compatible with $representation = ${this.content}." }

        if (newContent != this.content) tell("$representation = $newContent")

        this.content = newContent
    }

    override fun toString(): String {
        return "$representation : $content"
    }

    override fun hashCode(): Int {
        return super.hashCode() + content.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other === this
    }

    class Template<T : PValue<T>>(
        val type: KClass<T>,
        val name: String,
        private val notation: Notation,
        private val initialContent: Quantity<T> = AnyQuantity(type),
        private val normalizer: (Quantity<T>) -> Quantity<T> = ::noop
    ) {
        @Suppress("unused")
        sealed class Notation(internal val default: String, internal val custom: (String) -> String) {
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

        internal fun create(content: Quantity<*> = AnyQuantity(type), ownerName: String? = null): Field<T> {
            val representation = if (ownerName == null) notation.default else notation.custom(ownerName)
            val initialContent = content.toQuantity(type) intersect this.initialContent
            return Field(name, type, representation, initialContent, normalizer).apply { setContent(content) }
        }
    }
}
