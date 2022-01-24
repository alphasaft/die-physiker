package physics.quantities

import ofWhich
import kotlin.reflect.KClass


@JvmName("asPValueOrNullGeneric")
fun <V : PValue<V>> Quantity<V>.asPValueOrNull() = simplify().let { if (it is PValue<V>) it else null }
fun Quantity<*>.asPValueOrNull(): PValue<*>? = simplify().let { if (it is PValue<*>) it else null }

@JvmName("asPValueOrGeneric")
fun <V : PValue<V>> Quantity<V>.asPValueOr(default: PValue<V>) = asPValueOrNull() ?: default
fun Quantity<*>.asPValueOr(default: PValue<*>) = asPValueOrNull() ?: default

@JvmName("asPValueGeneric")
fun <V : PValue<V>> Quantity<V>.asPValue() = requireNotNull(asPValueOrNull()) { "Can't convert $this to a PValue." }
fun Quantity<*>.asPValue() = requireNotNull(asPValueOrNull()) { "Can't convert $this to a PValue." }

inline fun <reified V : PValue<V>> Quantity<*>.castAs(): Quantity<V> = castAs(V::class)
fun <V : PValue<V>> Quantity<*>.castAs(type: KClass<V>): Quantity<V> {
    require(this.type == type) { "Can't cast Quantity<${this.type.simpleName}> to Quantity<${type.simpleName}>." }
    return (@Suppress("UNCHECKED_CAST") (this as Quantity<V>))
}

