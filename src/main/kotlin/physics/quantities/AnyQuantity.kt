package physics.quantities

import kotlin.reflect.KClass


class AnyQuantity<V : PValue<V>>(override val type: KClass<V>) : Quantity<V> {
    companion object Factory {
        inline operator fun <reified V : PValue<V>> invoke() = AnyQuantity(V::class)
    }

    override fun contains(value: V): Boolean = true
    override fun simpleIntersect(quantity: Quantity<V>): Quantity<V> = quantity
    override fun simpleUnion(quantity: Quantity<V>): Quantity<V> = this
    override fun simplify(): Quantity<V> = this

    override fun toString(): String = "<?>"
    override fun equals(other: Any?): Boolean = other is AnyQuantity<*> && other.type == type
    override fun hashCode(): Int = AnyQuantity::class.hashCode() * 31 + type.hashCode()
}
