package physics

import Mapper
import Predicate
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sign
import kotlin.reflect.KClass


internal fun Double.roundAt(n: Int): Double {
    if (abs(n) > 1000) return this

    val unsignedRounded = "0".repeat(abs(n)) + (this*10.0.pow(-n)).roundToLong().toString().removePrefix("-")
    return (if (n >= 0) (unsignedRounded + "0".repeat(n))
    else (unsignedRounded.dropLast(-n) + ".") + unsignedRounded.takeLast(-n)).toDouble() * sign(this)
}

internal fun String.titlecase(): String {
    val words = split(" ").filterNot { it.isEmpty() }
    return words.joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
}

@Suppress("UNUSED_PARAMETER")
internal fun alwaysTrue(x: Any?): Boolean = true
@Suppress("UNUSED_PARAMETER")
internal fun alwaysTrue(x: Any?, y: Any?) = true
internal fun <T> noop(x: T): T = x

internal fun <T> chain(initial: T, vararg mappers: Mapper<T>): T = chain(initial, mappers.toList())
internal fun <T> chain(initial: T, mappers: List<Mapper<T>>): T {
    var result = initial
    for (mapper in mappers) {
        result = mapper(result)
    }
    return result
}

internal class UnorderedList<out E>(private val elements: List<E>) : List<E> by elements {
    override fun equals(other: Any?): Boolean {
        if (other !is UnorderedList<*>) return false
        val remainingElements = elements.toMutableList()
        for (element in other) {
            if (!remainingElements.remove(element)) return false
        }
        return remainingElements.isEmpty()
    }

    override fun hashCode(): Int {
        return elements.sortedBy { it.hashCode() }.hashCode()
    }

    override fun toString(): String {
        return elements.toString()
    }
}

internal fun binomialCoefficient(k: Int, n: Int): Int {
    return factorial(n) / (factorial(k) * factorial(n-k))
}

internal fun factorial(n: Int): Int {
    return if (n == 0) 1
    else n * factorial(n-1)
}

internal fun Double.isInt(): Boolean {
    return this.toInt().toDouble() == this
}

internal infix fun <E> Collection<E>.amputatedOf(other: Iterable<E>): List<E> {
    val result = toMutableList()
    for (item in other) result.remove(item)
    return result

}



