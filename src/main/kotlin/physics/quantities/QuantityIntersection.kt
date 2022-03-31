package physics.quantities

import kotlin.reflect.KClass


class QuantityIntersection<V : PValue<V>> private constructor(
    override val type: KClass<V>,
    val items: Set<Quantity<V>>,
) : Quantity<V> {

    companion object Factory {
        inline fun <reified V : PValue<V>> assertReduced(vararg items: Quantity<V>) = assertReduced(V::class, *items)
        fun <V : PValue<V>> assertReduced(type: KClass<V>, vararg items: Quantity<V>) = QuantityIntersection(type, items.toSet())

        inline fun <reified V : PValue<V>> new(vararg items: Quantity<V>): Quantity<V> = new(V::class, *items)
        fun <V : PValue<V>> new(type: KClass<V>, vararg items: Quantity<V>): Quantity<V> = new(type, items.toSet())

        inline fun <reified V : PValue<V>> new(items: List<Quantity<V>>): Quantity<V> = new(V::class, items)
        fun <V : PValue<V>> new(type: KClass<V>, items: List<Quantity<V>>): Quantity<V> = new(type, items.toSet())

        inline fun <reified V : PValue<V>> new(items: Set<Quantity<V>>) = new(V::class, items)
        fun <V : PValue<V>> new(type: KClass<V>, items: Set<Quantity<V>>): Quantity<V> = QuantityIntersection(type, items).simplify()
    }

    override fun toString() = items.joinToString(" && ")

    override fun equals(other: Any?): Boolean {
        return other is QuantityIntersection<*> && items == other.items
    }

    override fun hashCode(): Int {
        return items.hashCode()
    }

    override fun contains(value: V): Boolean = items.all { value in it }

    override fun stdIntersect(quantity: Quantity<V>): Quantity<V> = new(type, items + quantity)

    override fun stdUnion(quantity: Quantity<V>): Quantity<V> = new(type, items.map { it union quantity })

    override fun simplify(): Quantity<V> {
        return when {
            items.isEmpty() -> AnyQuantity(type)
            items.size == 1 -> items.single()
            else -> reduceItems()
        }
    }

    private fun reduceItems(): Quantity<V> {
        if (items.any { it is ImpossibleQuantity<V> }) return ImpossibleQuantity(type)

        val items = items
            .filterNot { it is AnyQuantity<V> }
            .map { if (it is QuantityIntersection) it.items else listOf(it) }
            .flatten()

        for ((i, x) in items.withIndex()) {
            for (y in items.drop(i+1)) {
                val intersect = x stdIntersect y
                if (intersect !is QuantityIntersection) return new(type, items - y - x + intersect)
            }
        }

        return this
    }

    fun mapItems(mapper: (Quantity<V>) -> Quantity<V>): Quantity<V> {
        return new(type, items.map(mapper))
    }
}
