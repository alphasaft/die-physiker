package physics.quantities

import kotlin.reflect.KClass


class ImpossibleQuantity<V : PValue<V>>(override val type: KClass<V>) : Quantity<V> {
    companion object Factory {
        inline operator fun <reified V : PValue<V>> invoke() = ImpossibleQuantity(V::class)
    }

    override fun simpleIntersect(quantity: Quantity<V>): ImpossibleQuantity<V> = this
    override fun simpleUnion(quantity: Quantity<V>): Quantity<V> = quantity
    override fun contains(value: V): Boolean = false
    override fun simplify(): Quantity<V> = this

    override fun toString(): String = "</>"
    override fun equals(other: Any?): Boolean = other is ImpossibleQuantity<*> && type == other.type
    override fun hashCode(): Int = ImpossibleQuantity::class.hashCode() * 31 + type.hashCode()
}
