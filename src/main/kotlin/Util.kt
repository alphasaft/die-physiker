
fun println(vararg args: Any?, separator: String = " ") {
    kotlin.io.println(args.joinToString(separator))
}

fun cwd(): String = System.getProperty("user.dir") + "\\src\\main"

@Suppress("UNUSED_PARAMETER")
fun alwaysTrue(x: Any?) = true

@Suppress("UNUSED_PARAMETER")
fun alwaysTrue(x: Any?, y: Any?) = true

fun <T> noop(x: T): T = x

fun <T> safe(x: Any): T = @Suppress("UNCHECKED_CAST") (x as T)

fun binomialCoefficient(k: Int, n: Int): Int {
    return factorial(n) / (factorial(k) * factorial(n-k))
}

fun factorial(n: Int): Int {
    return if (n == 0) 1
    else n * factorial(n-1)
}

internal class UnorderedList<out E>(private val elements: List<E>) : Collection<E> by elements {

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
