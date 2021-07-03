package physics

import kotlin.reflect.KClass


class FormulaArguments(private val storage: Map<String, PhysicalComponent>) {
    inline operator fun <reified T : Any> get(componentDotField: String): T = get(T::class, componentDotField)

    fun displayStorage() {
        println(storage)
    }

    fun <T : Any> get(kClass: KClass<T>, componentDotField: String): T {
        return storage.getValue(componentDotField.split(".").first()).getField(kClass, componentDotField.split(".").last())
            ?: throw NoSuchElementException("Value of field $componentDotField is undeclared or unknown.")
    }

    inline fun <reified T : Any> getAll(componentDotField: String): List<T> = getAll(T::class, componentDotField)
    fun <T : Any> getAll(kClass: KClass<T>, componentDotField: String): List<T> {
        var i = 1
        val result = mutableListOf<T>()
        val component = componentDotField.split(".").first()
        val field = componentDotField.split(".").last()

        while (true) {
            try {
                result.add(get(kClass, "$component${i++}.$field"))
            } catch (e: NoSuchElementException) {
                return result
            }
        }
    }
}
