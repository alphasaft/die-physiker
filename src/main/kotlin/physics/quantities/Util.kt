package physics.quantities

import assert
import kotlin.reflect.KClass

private fun <V : PValue<V>> PValue<V>.unwrap(): V {
    return this.assert<V>()
}

@JvmName("asPValueOrNullGeneric")
fun <V : PValue<V>> Quantity<V>.asPValueOrNull(): V? = simplify().let { if (it is PValue<V>) it else null }?.unwrap()
fun Quantity<*>.asPValueOrNull(): PValue<*>? = simplify().let { if (it is PValue<*>) it else null }

@JvmName("asPValueOrGeneric")
inline fun <V : PValue<V>> Quantity<V>.asPValueOrElse(default: () -> V): V = asPValueOrNull<V>() ?: default()
inline fun Quantity<*>.asPValueOrElse(default: () -> PValue<*>) = asPValueOrNull() ?: default()

@JvmName("asPValueGeneric")
fun <V : PValue<V>> Quantity<V>.asPValue(): V = requireNotNull(asPValueOrNull<V>()) { "Can't convert $this to a PValue." }
fun Quantity<*>.asPValue() = requireNotNull(asPValueOrNull()) { "Can't convert $this to a PValue." }

inline fun <reified V : PValue<V>> Quantity<*>.toQuantity(): Quantity<V> = toQuantity(V::class)
fun <V : PValue<V>> Quantity<*>.toQuantity(type: KClass<V>): Quantity<V> {
    return when (this) {
        is PValue -> toPValue(type)
        is QuantityUnion -> mapItemsWithNewType(type) { it.toQuantity(type) }
        is QuantityIntersection -> mapItemsWithNewType(type) { it.toQuantity(type) }
        is AnyQuantity -> AnyQuantity(type)
        is ImpossibleQuantity -> ImpossibleQuantity(type)
        else -> {
            require(this.type == type) { "Can't cast Quantity<${this.type.simpleName}> to Quantity<${type.simpleName}>." }
            (@Suppress("UNCHECKED_CAST") (this as Quantity<V>))
        }
    }


}
