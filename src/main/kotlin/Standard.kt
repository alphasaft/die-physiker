
fun println(vararg args: Any?, separator: String = " ") {
    kotlin.io.println(args.joinToString(separator))
}

fun cwd(): String = System.getProperty("user.dir") + "\\src\\main"

