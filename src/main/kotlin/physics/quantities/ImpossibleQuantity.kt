package physics.quantities

import kotlin.reflect.KClass


class ImpossibleQuantity<V : PValue<V>>(override val type: KClass<V>) : Quantity<V> {
    companion object Factory {
        inline operator fun <reified V : PValue<V>> invoke() = ImpossibleQuantity(V::class)
    }

    override fun intersect(quantity: Quantity<V>): ImpossibleQuantity<V> = this
    override fun union(quantity: Quantity<V>): Quantity<V> = quantity
    override fun contains(value: V): Boolean = false
    override fun toString(): String = "Valeur impossible."
    override fun simplify(): Quantity<V> = this
}
