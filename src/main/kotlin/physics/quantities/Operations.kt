package physics.quantities



infix fun <V : PValue<V>> Quantity<V>.includedIn(other: Quantity<V>): Boolean =
    this union other == other || this intersect other == this

infix fun <V : PValue<V>> Quantity<V>.includes(other: Quantity<V>): Boolean =
    this union other == this || this intersect other == other


private fun <T, R> commutativeOperation(x: T, y: T, operation: (a: T, b: T) -> R, reducer: (R, R) -> R): R =
    reducer(operation(x, y), operation(y, x))


infix fun <V : PValue<V>> Quantity<V>.union(other: Quantity<V>): Quantity<V> {
    return commutativeOperation(
        this,
        other,
        operation = Quantity<V>::simpleUnion,
        reducer = { r1, r2 ->
            when {
                r1 is ImpossibleQuantity -> r2
                r1 is QuantityUnion && r2 !is ImpossibleQuantity -> r2
                r2 is AnyQuantity -> r2
                else -> r1
            }
        }
    )
}

infix fun <V : PValue<V>> Quantity<V>.intersect(other: Quantity<V>): Quantity<V> {
    return commutativeOperation(
        this,
        other,
        operation = Quantity<V>::simpleIntersect,
        reducer = { r1, r2 ->
            when {
                r1 is AnyQuantity -> r2
                r1 is QuantityIntersection && r2 !is AnyQuantity -> r2
                r2 is ImpossibleQuantity -> r2
                else -> r1
            }
        }
    )
}


fun <V : PValue<V>> Quantity<V>.toPSet(): PSet<V> {
    val quantity = this
    return PSet(type) { it in quantity }
}