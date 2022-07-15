package physics.quantities


import kotlin.reflect.KClass


sealed class PValue<T : PValue<T>> : Quantity<T> {
    private val that get() = @Suppress("UNCHECKED_CAST") (this as T)

    abstract fun toPBoolean(): PBoolean
    abstract fun toPInt(): PInt
    abstract fun toPReal(): PReal
    abstract fun toPString(): PString

    inline fun <reified V : PValue<V>> useAs(): V = this.useAs(V::class)
    fun <V : PValue<V>> useAs(kClass: KClass<V>): V {
        return (@Suppress("UNCHECKED_CAST") (when (kClass) {
            PBoolean::class -> toPBoolean()
            PInt::class -> toPInt()
            PReal::class -> toPReal()
            PString::class -> toPString()
            else -> throw NoWhenBranchMatchedException("Unexpected cast type ${kClass.simpleName}")
        } as V))
    }

    override fun contains(value: T): Boolean = value == that

    override fun simpleIntersect(quantity: Quantity<T>): Quantity<T> =
        if (that in quantity) this else ImpossibleQuantity(type)

    override fun simpleUnion(quantity: Quantity<T>): Quantity<T> =
        if (that in quantity) quantity else QuantityUnion.assertReduced(type, this, quantity)

    override fun simplify(): Quantity<T> =
        this

    override fun toString(): String =
        that.toString()

}
