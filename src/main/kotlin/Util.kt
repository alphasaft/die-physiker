
fun println(vararg args: Any?, separator: String = " ") {
    kotlin.io.println(args.joinToString(separator))
}

fun cwd(): String = System.getProperty("user.dir") + "\\src\\main"


@Suppress("UNUSED_PARAMETER")
fun alwaysTrue(x: Any?, y: Any?) = true

fun <T> noop(x: T): T = x

fun binomialCoefficient(k: Int, n: Int): Int {
    return factorial(n) / (factorial(k) * factorial(n-k))
}

fun factorial(n: Int): Int {
    return if (n == 0) 1
    else n * factorial(n-1)
}
