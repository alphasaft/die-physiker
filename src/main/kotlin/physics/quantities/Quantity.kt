package physics.quantities

import kotlin.reflect.KClass


interface Quantity<V : PValue<V>> {
    val type: KClass<V>
    infix fun intersect(quantity: Quantity<V>): Quantity<V>
    infix fun union(quantity: Quantity<V>): Quantity<V>
    operator fun contains(value: V): Boolean
    override fun toString(): String
    fun simplify(): Quantity<V>
}


