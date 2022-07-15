package physics.quantities

import kotlin.reflect.KClass


interface Quantity<V : PValue<V>> {
    val type: KClass<V>
    infix fun simpleIntersect(quantity: Quantity<V>): Quantity<V>
    infix fun simpleUnion(quantity: Quantity<V>): Quantity<V>
    operator fun contains(value: V): Boolean
    fun simplify(): Quantity<V>

    override fun toString(): String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}


