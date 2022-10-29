package physics.packaging

import mergeWith
import physics.components.ComponentClass

class ModuleBuilder(private val name: String, private val modules: Map<String, UnloadedModule>) {
    private val imported = mutableListOf<String>()
    private val unqualifiedClasses = mutableMapOf<String, ComponentClass>()
    private val qualifiedModules = mutableMapOf<String, Module>()

    private val localClasses = mutableMapOf<String, ComponentClass>()
    private val flags = mutableListOf<String>()

    init {
        if (isAvailable("base")) use("base")
    }


    private fun traceImport(moduleName: String) {
        imported.add(moduleName)
    }

    private fun requireCanImport(moduleName: String) {
        require(moduleName in modules) { "Can't find module '$moduleName'" }
        require(moduleName !in imported) { "Module was already imported." }
    }

    fun isAvailable(moduleName: String): Boolean {
        return moduleName in modules
    }

    fun use(moduleName: String) {
        requireCanImport(moduleName)

        traceImport(moduleName)
        unqualifiedClasses.mergeWith(
            modules.getValue(moduleName).load().unpack(),
            merge = { k, _, _ -> throw IllegalArgumentException("When importing '$moduleName' : Crashing declarations for name '$k'. Try useQualified instead.") }
        )
    }

    fun useQualified(moduleName: String) {
        useQualifiedAs(moduleName, alias = moduleName)
    }

    fun useQualifiedAs(moduleName: String, alias: String) {
        requireCanImport(moduleName)

        traceImport(moduleName)
        qualifiedModules[alias] = modules.getValue(moduleName).load()
    }


    fun flag(name: String) {
        flags.add(name)
    }


    operator fun String.invoke(): ComponentClass {
        return if ('.' in this) getClassQualified(dropLastWhile { it != '.' }.dropLast(1), takeLastWhile { it != '.' })
        else getClassUnqualified(this)
    }

    private fun getClassQualified(moduleName: String, className: String): ComponentClass {
        require(moduleName in qualifiedModules) { "Module $moduleName wasn't imported as qualified. Perhaps you meant useQualified instead of use ?" }

        return qualifiedModules.getValue(moduleName)[className]
    }

    private fun getClassUnqualified(className: String): ComponentClass {
        require(className in unqualifiedClasses) { "No such class : $className." }

        return unqualifiedClasses.getValue(className)
    }


    operator fun String.rangeTo(c: ComponentClass) {
        localClasses[this] = c
        unqualifiedClasses[this] = c
    }


    internal fun build(): Module {
        return Module(name, localClasses)
    }
}