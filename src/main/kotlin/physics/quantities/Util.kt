package physics.quantities

import kotlin.reflect.KClass

private fun <V : PValue<V>> PValue<V>.unwrap(): V {
    return (@Suppress("UNCHECKED_CAST") (this as V))
}

@JvmName("asPValueOrNullGeneric")
fun <V : PValue<V>> Quantity<V>.asPValueOrNull(): V? = simplify().let { if (it is PValue<V>) it else null }?.unwrap()
fun Quantity<*>.asPValueOrNull(): PValue<*>? = simplify().let { if (it is PValue<*>) it else null }

@JvmName("asPValueOrGeneric")
fun <V : PValue<V>> Quantity<V>.asPValueOr(default: V): V = asPValueOrNull<V>() ?: default
fun Quantity<*>.asPValueOr(default: PValue<*>) = asPValueOrNull() ?: default

@JvmName("asPValueGeneric")
fun <V : PValue<V>> Quantity<V>.asPValue(): V = requireNotNull(asPValueOrNull<V>()) { "Can't convert $this to a PValue." }
fun Quantity<*>.asPValue() = requireNotNull(asPValueOrNull()) { "Can't convert $this to a PValue." }

inline fun <reified V : PValue<V>> Quantity<*>.castAs(): Quantity<V> = castAs(V::class)
fun <V : PValue<V>> Quantity<*>.castAs(type: KClass<V>): Quantity<V> {
    require(this.type == type) { "Can't cast Quantity<${this.type.simpleName}> to Quantity<${type.simpleName}>." }
    return (@Suppress("UNCHECKED_CAST") (this as Quantity<V>))
}
