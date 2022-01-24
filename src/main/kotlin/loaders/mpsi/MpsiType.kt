package loaders.mpsi

import physics.quantities.PValue
import kotlin.reflect.KClass


internal sealed class MpsiType {
    abstract val members: kotlin.collections.List<MpsiType>

    object Infer : MpsiType() {
        override val members: kotlin.collections.List<MpsiType> = emptyList()

        override fun toString(): String {
            return "Infer"
        }
    }

    object None : MpsiType() {
        override val members: kotlin.collections.List<MpsiType> = emptyList()

        override fun toString(): String {
            return "None"
        }
    }

    class Builtin(val builtinType: KClass<out PValue<*>>) : MpsiType() {
        override val members = emptyList<MpsiType>()

        override fun equals(other: Any?): Boolean {
            return other is Builtin && other.builtinType == builtinType
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + builtinType.hashCode()
            result = 31 * result + members.hashCode()
            return result
        }

        override fun toString(): String {
            return builtinType.simpleName!!.removePrefix("Physical")
        }
    }

    class Component(private val componentClassName: String) : MpsiType() {
        override val members: kotlin.collections.List<MpsiType> = emptyList()

        override fun toString(): String {
            return componentClassName
        }
    }

    class Tuple(private val items: kotlin.collections.List<MpsiType>) : MpsiType() {
        override val members = items

        override fun toString(): String {
            return "(${items.joinToString(", ")})"
        }
    }

    class List(private val itemType: MpsiType) : MpsiType() {
        override val members = listOf(itemType)

        override fun toString(): String {
            return "[$itemType]"
        }
    }

    class Map(private val keyType: Builtin, private val valueType: MpsiType) : MpsiType() {
        override val members = listOf(keyType, valueType)

        override fun toString(): String {
            return "{$keyType: $valueType}"
        }
    }

    class Nullable(type: MpsiType) : MpsiType() {
        private val type: MpsiType = if (type is Nullable) type.type else type
        override val members: kotlin.collections.List<MpsiType> = listOf(type)

        override fun toString(): String {
            return "?$type"
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is MpsiType && other::class === this::class && members == other.members
    }

    override fun hashCode(): Int {
        return members.hashCode()
    }
}
