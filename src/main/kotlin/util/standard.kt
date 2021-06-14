package util

fun <T> noop(obj: T) = obj
fun <T> buildList(builder: MutableList<T>.() -> Unit) = mutableListOf<T>().apply(builder).toList()
