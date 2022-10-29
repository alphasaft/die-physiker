package physics.packaging

import mergeWith


class CompilationUnit {
    private val modules = mutableMapOf<String, UnloadedModule>()

    fun module(moduleName: String, script: ModuleBuilder.() -> Unit) {
        modules[moduleName] = lazy { ModuleBuilder(moduleName, modules).apply(script).build() }
    }

    fun getModule(moduleName: String): Module {
        require(moduleName in modules) { "No such module : $moduleName" }

        return modules.getValue(moduleName).load()
    }

    fun fuseWith(other: CompilationUnit) {
        modules.mergeWith(
            other.modules,
            merge = { name, m1, m2 ->
                if (m1 === m2) m1
                else throw IllegalArgumentException("When merging compilations : Crash on name '$name'.")
            }
        )
    }

    fun asModule(name: String = "<main>"): Module {
        return modules.values.fold(Module(name, emptyMap())) { m1, m2 -> m1 + m2.load() }
    }
}


fun startCompilation(): CompilationUnit = CompilationUnit()

fun startCompilationWith(vararg compilations: CompilationUnit) = CompilationUnit().apply {
    for (compilation in compilations) {
        fuseWith(compilation)
    }
}
