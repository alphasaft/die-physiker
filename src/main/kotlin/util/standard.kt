package util

import util.xml.Node
import util.xml.name

fun <T> noop(obj: T) = obj

fun <T> buildList(builder: MutableList<T>.() -> Unit) = mutableListOf<T>().apply(builder).toList()

fun <T : Any> generate(first: T?, next: (T) -> T?): List<T> {
    if (first == null) return emptyList()
    var current: T = first
    val result = mutableListOf<T>()
    while (true) {
        result.add(current)
        current = next(current) ?: break
    }
    return result
}

fun println(vararg args: Any?, separator: String = ", ") {
    kotlin.io.println(args.joinToString(separator))
}

