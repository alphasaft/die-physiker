package physics.quantities

import kotlin.reflect.KClass


class AnyQuantity<V : PValue<V>>(override val type: KClass<V>) : Quantity<V> {
    companion object Factory {
        inline operator fun <reified V : PValue<V>> invoke() = AnyQuantity(V::class)
    }

    override fun contains(value: V): Boolean = true
    override fun intersect(quantity: Quantity<V>): Quantity<V> = quantity
    override fun union(quantity: Quantity<V>): Quantity<V> = this
    override fun toString(): String = "N'importe quelle valeur."
    override fun simplify(): Quantity<V> = this
}
