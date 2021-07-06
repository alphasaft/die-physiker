package loaders.base

import java.io.File

abstract class DataLoader<P : Parser, O>(private val parser: P) {
    fun loadFrom(file: File): O = loadFrom(file.readText())
    fun loadFrom(input: String): O {
        val ast = parser.parse(input)
        return generateFrom(ast)
    }

    abstract fun generateFrom(ast: Ast): O
}
