package physics.quantities

import Predicate
import kotlin.reflect.KClass


class PSet<V : PValue<V>>(
    override val type: KClass<V>,
    private val ownershipPredicate: Predicate<V>,
) : Quantity<V> {

    companion object Factory {
        inline operator fun <reified V : PValue<V>> invoke(noinline ownershipPredicate: Predicate<V>) =
            PSet(V::class, ownershipPredicate)
    }

    override fun contains(value: V): Boolean = ownershipPredicate(value)

    override fun simpleIntersect(quantity: Quantity<V>): Quantity<V> {
        if (quantity is PSet<V>) return PSet(type) { it in this && it in quantity }
        return AnyQuantity(type)
    }

    override fun simpleUnion(quantity: Quantity<V>): Quantity<V> {
        if (quantity is PSet<V>) return PSet(type) { it in this || it in quantity }
        return AnyQuantity(type)
    }

    override fun simplify(): Quantity<V> {
        return this
    }

    override fun toString(): String {
        return "{ ... }"
    }

    override fun equals(other: Any?): Boolean {
        return other === this
    }

    override fun hashCode(): Int {
        return ownershipPredicate.hashCode()
    }
}
