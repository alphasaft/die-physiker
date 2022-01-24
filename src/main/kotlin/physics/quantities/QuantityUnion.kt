package physics.quantities

import kotlin.reflect.KClass


open class QuantityUnion<V : PValue<V>> private constructor(
    override val type: KClass<V>,
    val items: Set<Quantity<V>>,
) : Quantity<V> {
    companion object Factory {
        inline fun <reified V : PValue<V>> assertReduced(vararg items: Quantity<V>): QuantityUnion<V> = assertReduced(V::class, *items)
        fun <V : PValue<V>> assertReduced(type: KClass<V>, vararg items: Quantity<V>) = QuantityUnion(type, items.toSet())

        inline fun <reified V : PValue<V>> new(vararg items: Quantity<V>): Quantity<V> = new(items.toSet())
        fun <V : PValue<V>> new(type: KClass<V>, vararg items: Quantity<V>): Quantity<V> = new(type, items.toSet())

        inline fun <reified V : PValue<V>> new(items: List<Quantity<V>>): Quantity<V> = new(items.toSet())
        fun <V : PValue<V>> new(type: KClass<V>, items: List<Quantity<V>>): Quantity<V> = new(type, items.toSet())

        inline fun <reified V : PValue<V>> new(items: Set<Quantity<V>>): Quantity<V> = new(V::class, items)
        fun <V : PValue<V>> new(type: KClass<V>, items: Set<Quantity<V>>): Quantity<V> = QuantityUnion(type, items).simplify()
    }

    override fun toString(): String = items.joinToString(" || ")

    override fun contains(value: V): Boolean = items.any { value in it }

    override fun intersect(quantity: Quantity<V>): Quantity<V> {
        return new(type, items.map { it intersect quantity })
    }

    override fun union(quantity: Quantity<V>): Quantity<V> {
        return new(type, items + quantity)
    }

    override fun simplify(): Quantity<V> {
        return when {
            items.isEmpty() -> ImpossibleQuantity(type)
            items.size == 1 -> items.single()
            else -> reduceItems()
        }
    }

    private fun reduceItems(): Quantity<V> {
        if (items.any { it is AnyQuantity<V> }) return AnyQuantity(type)

        val items = items
            .filterNot { it is ImpossibleQuantity<*> }
            .map { if (it is QuantityUnion) it.items else listOf(it) }
            .flatten()

        for ((i, x) in items.withIndex()) {
            for (y in items.drop(i+1)) {
                val union = x union y
                if (union !is QuantityUnion) return new(type, items - y - x + union)
            }
        }

        return this
    }
}
