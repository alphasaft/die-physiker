package physics.quantities

import println


infix fun <V : PValue<V>> Quantity<V>.includedIn(other: Quantity<V>): Boolean =
    this union other == other || this intersect other == this

infix fun <V : PValue<V>> Quantity<V>.includes(other: Quantity<V>): Boolean =
    this union other == this || this intersect other == other


private fun <T> commutativeOperation(x: T, y: T, reducer: (T, T) -> T, operation: (a: T, b: T) -> T): T =
    reducer(operation(x, y), operation(y, x))


infix fun <V : PValue<V>> Quantity<V>.union(other: Quantity<V>): Quantity<V> {
    return commutativeOperation(this, other, reducer = { r1, r2 -> if (r1 is QuantityUnion) r2 else r1 }) { a, b ->
        when (a) {
            b -> a
            is AnyQuantity -> a
            is ImpossibleQuantity -> b
            is QuantityUnion -> QuantityUnion.new(a.type, listOf(a, b))
            is QuantityIntersection -> QuantityIntersection.new(a.type, a.items.map { it union b })
            is PValue -> if (a.asPValue<V>() in b) b else QuantityUnion.assertReduced(a.type, a, b)
            else -> a stdUnion b
        }
    }
}

infix fun <V : PValue<V>> Quantity<V>.intersect(other: Quantity<V>): Quantity<V> {
    return commutativeOperation(this, other, reducer = { r1, r2 -> if (r1 is QuantityIntersection) r2 else r1 }) { a, b ->
        when (a) {
            b -> a
            is AnyQuantity -> b
            is ImpossibleQuantity -> a
            is QuantityIntersection -> QuantityIntersection.new(a.type, listOf(a, b))
            is QuantityUnion -> QuantityUnion.new(a.type, *a.items.map { it intersect b }.toTypedArray())
            is PValue -> if (a.asPValue<V>() in b) a else ImpossibleQuantity(a.type)
            else -> a stdIntersect b
        }
    }
}
