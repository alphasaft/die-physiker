import java.util.*
import kotlin.math.pow

internal fun <T> T.ofWhich(check: T.() -> Boolean): T {
    require(check()) { "Constraint wasn't fulfilled for object $this" }
    return this
}

internal fun <T> T.ofWhichOrNull(check: T.() -> Boolean): T? {
    return if (check()) this else null
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


internal fun <T> List<T>.subList(startIndex: Int) = subList(startIndex, size)

internal fun <A, B> List<Pair<A, B>>.toMutableMap() = toMap().toMutableMap()


internal operator fun <T, R> List<T>.times(other: List<R>): List<Pair<T, R>> {
    val result = mutableListOf<Pair<T, R>>()
    for (item1 in this) for (item2 in other) result.add(item1 to item2)
    return result
}

internal fun <K, V> MutableMap<K, V>.mergeWith(other: Map<K, V>, merge: (key: K, old: V, new: V) -> V) {
    for ((k, v) in other) {
        this[k] = if (containsKey(k)) merge(k, this.getValue(k), v) else v
    }
}

internal fun <K, V> MutableMap<K, V>.mergeWith(other: Map<K, V>, merge: (old: V, new: V) -> V) {
    for ((k, v) in other) {
        this[k] = if (containsKey(k)) merge(this.getValue(k), v) else v
    }
}

internal fun <K, V> Map<K, V>.mergedWith(other: Map<K, V>, merge: (key: K, old: V, new: V) -> V): MutableMap<K, V> {
    val result = this.toMutableMap()
    result.mergeWith(other, merge)
    return result
}

internal fun <K, V> Map<K, V>.mergedWith(other: Map<K, V>, merge: (old: V, new: V) -> V): MutableMap<K, V> {
    val result = this.toMutableMap()
    result.mergeWith(other, merge)
    return result
}

internal fun Double.suppressDwindlingDigits(): Double {
    var asString = toString()

    if (asString.split(".").last().length < 10) return this

    val splitOnE = asString.split("E")
    if (splitOnE.size > 1) {
        val (coefficient, exponent) = splitOnE.first() to splitOnE.last().toInt()
        asString = if (exponent < 0) {
            ("0." + "0".repeat(-exponent - 1) + coefficient.remove(".")).removeSuffix("0")
        } else {
            coefficient.remove(".") + "0".repeat(exponent-1)
        }
    }

    return if (asString.last().digitToInt() < 5) asString.dropLast(1).dropLastWhile { it in ".0" }.toDouble()
    else asString.dropLast(1).dropLastWhile { it in ".9" }.let { it.dropLast(1) + (it.last().digitToInt()+1) }.toDouble()
}

internal fun Boolean.toInt() = if (this) 1 else 0
