

fun <T> T.ofWhich(check: T.() -> Boolean): T {
    require(check()) { "Constraint wasn't fulfilled for object $this" }
    return this
}

fun <T> T.ofWhichOrNull(check: T.() -> Boolean): T? {
    return if (check()) this else null
}

fun String.remove(s: String): String = replace(s, "")
fun String.swap(s1: String, s2: String): String {
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

fun String.normalize(): String {
    var result = toLowerCase()
    for ((charsToNormalize, normalized) in NORMALIZING_MAP) {
        for (char in charsToNormalize) {
            result = result.replace(char, normalized)
        }
    }
    return result
}


fun <T> List<T>.subList(startIndex: Int) = subList(startIndex, size)

fun <A, B> List<Pair<A, B>>.toMutableMap() = toMap().toMutableMap()

@JvmName("flatListTimes")
operator fun <T> List<T>.times(other: List<T>) = map { listOf(it) } * other

operator fun <T> List<List<T>>.times(other: List<T>): List<List<T>> {
    val result = mutableListOf<List<T>>()
    for (subList in this) {
        for (item in other) {
            result.add(subList.plusElement(item))
        }
    }
    return result
}

fun <K, V> MutableMap<K, V>.mergeWith(other: Map<K, V>, merge: (old: V, new: V) -> V) {
    for ((k, v) in other) this[k] = if (containsKey(k)) merge(this.getValue(k), v) else v
}
