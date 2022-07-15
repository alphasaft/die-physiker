import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sign

internal fun <T> T.ofWhich(check: T.() -> Boolean): T {
    require(check()) { "Constraint wasn't fulfilled for object $this" }
    return this
}

internal fun <T> T.ofWhichOrNull(check: T.() -> Boolean): T? {
    return if (check()) this else null
}

internal inline fun <reified T : Any> Any?.ensure(): T {
    require(this is T) { "Expected ${T::class.simpleName}, got $this." }
    return this
}

internal fun <T> Any?.assert(): T {
    @Suppress("UNCHECKED_CAST")
    return this as T
}

internal infix fun Boolean.but(other: Boolean) =
    this && other

internal fun Double.roundAt(n: Int): Double {
    if (abs(n) > 1000) return this

    val unsignedRounded = "0".repeat(abs(n)) + (this*10.0.pow(-n)).roundToLong().toString().removePrefix("-")
    return (if (n >= 0) (unsignedRounded + "0".repeat(n))
    else (unsignedRounded.dropLast(-n) + ".") + unsignedRounded.takeLast(-n)).toDouble() * sign(this)
}

internal fun Double.isInt(): Boolean {
    return this.toInt().toDouble() == this
}

internal fun String.titlecase(): String {
    val words = split(" ").filterNot { it.isEmpty() }
    return words.joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
}

internal fun String.remove(s: String): String = replace(s, "")

internal fun String.swap(s1: String, s2: String): String {
    var i = 0
    while ("[$i]" in this) {
        i++
    }
    return this
        .replace(s1, "[$i]")
        .replace(s2, s1)
        .replace("[$i]", s2)
}


private val NORMALIZING_MAP = listOf(
    listOf('à', 'â', 'ä') to 'a',
    listOf('é', 'è', 'ê', 'ë') to 'e',
    listOf('ô', 'ö') to 'o',
    listOf('û', 'ü') to 'u',
    listOf('î', 'ï') to 'i'
)

internal fun String.normalize(): String {
    var result = lowercase(Locale.getDefault())
    for ((charsToNormalize, normalized) in NORMALIZING_MAP) {
        for (char in charsToNormalize) {
            result = result.replace(char, normalized)
        }
    }
    return result
}

internal operator fun Pair<Int, Int>.plus(other: Pair<Int, Int>): Pair<Int, Int> {
    return Pair(first + other.first, second + other.second)
}

fun <T> Iterable<T>.filterOut(excluded: Iterable<T>): List<T> {
    return filter { item -> excluded.none { e -> e === item } }
}

inline fun <T, reified U : T> Iterable<T>.filterIsInstanceAndReplace(replacement: (List<U>) -> List<T>): List<T> {
    val replaced = this.filterIsInstance<U>()
    val unchanged = this.filterOut(replaced)
    return unchanged + if (replaced.isNotEmpty()) replacement(replaced) else emptyList()
}

internal fun <T> Collection<T>.getAllArrangements(): Set<List<T>> {
    return if (isEmpty()) setOf(emptyList())
    else drop(1).getAllArrangements().flatMap { c -> List(size) { i -> c.take(i) + first() + c.drop(i) } }.toSet()
}

infix fun <E> Collection<E>.amputatedOf(other: Iterable<E>): List<E> {
    val result = toMutableList()
    for (item in other) result.remove(item)
    return result
}

internal fun <K, V> List<Map<K, V>>.fuseAll(onFailure: (key: K, value1: V, value2: V) -> V): Map<K, V> {
    return reduce { acc, map -> acc.mergedWith(map, merge = onFailure) }
}

internal fun <T> Set<T>.getAllSubsets(): Set<Set<T>> {
    if (isEmpty()) return setOf(emptySet())
    val head = take(1)
    val tail = drop(1).toSet()
    return (tail.getAllSubsets() + tail.getAllSubsets().map { (head + it).toSet() }).toSet()
}

internal operator fun <T, R> List<T>.times(other: List<R>): List<Pair<T, R>> {
    val result = mutableListOf<Pair<T, R>>()
    for (item1 in this) for (item2 in other) result.add(item1 to item2)
    return result
}

internal fun <T> MutableList<T>.replace(old: T, new: T) {
    replaceAll { if (it === old) new else it }
}

internal infix fun <K, V> Map<K, V>.isIncludedIn(container: Map<K, V>): Boolean {
    return all { (k, v) -> k in container && container[k] == v }
}

internal inline fun <K, V> Map<K, V>.mergedWith(other: Map<K, V>, merge: (key: K, old: V, new: V) -> V): MutableMap<K, V> {
    val result = this.toMutableMap()
    result.mergeWith(other, merge)
    return result
}

internal inline fun <K, V> Map<K, V>.mergedWith(other: Map<K, V>, merge: (old: V, new: V) -> V): MutableMap<K, V> {
    val result = this.toMutableMap()
    result.mergeWith(other, merge)
    return result
}



internal inline fun <K, V> MutableMap<K, V>.mergeWith(other: Map<K, V>, merge: (key: K, old: V, new: V) -> V) {
    for ((k, v) in other) {
        this[k] = if (containsKey(k)) merge(k, this.getValue(k), v) else v
    }
}

internal inline fun <K, V> MutableMap<K, V>.mergeWith(other: Map<K, V>, merge: (old: V, new: V) -> V) {
    for ((k, v) in other) {
        this[k] = if (containsKey(k)) merge(this.getValue(k), v) else v
    }
}

