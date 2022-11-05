package physics.packaging

import mergeWith
import physics.components.ComponentClass


class ModuleBuilder(private val name: String, private val modules: Map<String, UnloadedModule>) {

    companion object {
        private const val BASE_MODULE_NAME = "base"
        private var moduleId = 1

        private fun allocateId(): Int {
            return moduleId++
        }

        internal fun moduleFromClasses(name: String, classes: Map<String, ComponentClass>): Module {
            return Module(name, allocateId(), classes)
        }
    }

    private val moduleId = allocateId()

    private val imported = mutableListOf<String>()
    private val unqualifiedClasses = mutableMapOf<String, ComponentClass>()
    private val qualifiedModules = mutableMapOf<String, Module>()

    private val localClasses = mutableMapOf<String, ComponentClass>()

    init {
        if (name != BASE_MODULE_NAME && isAvailable(BASE_MODULE_NAME)) use(BASE_MODULE_NAME)
    }


    private fun MutableMap<String, ComponentClass>.mergeWithModuleContents(contents: Map<String, ComponentClass>) {
        mergeWith(contents) { crashingName, c1, c2 ->
            require(c1 == c2) { "When merging modules : Crash on name $crashingName." }
            c1
        }
    }

    private fun traceImport(moduleName: String) {
        imported.add(moduleName)
    }

    private fun requireCanImport(moduleName: String) {
        require(moduleName in modules) { "Can't find module '$moduleName'" }
        require(moduleName !in imported) {
            if (moduleName == BASE_MODULE_NAME) "Module '$BASE_MODULE_NAME' is always imported by default."
            else "Module '$moduleName' was already imported."
        }
    }

    fun isAvailable(moduleName: String): Boolean {
        return moduleName in modules
    }

    fun use(moduleName: String) {
        requireCanImport(moduleName)

        traceImport(moduleName)
        unqualifiedClasses.mergeWithModuleContents(modules.getValue(moduleName).load().unpack(), )
    }

    fun useQualified(moduleName: String) {
        useQualifiedAs(moduleName, alias = moduleName)
    }

    fun useQualifiedAs(moduleName: String, alias: String) {
        requireCanImport(moduleName)

        traceImport(moduleName)
        qualifiedModules[alias] = modules.getValue(moduleName).load()
    }

    fun export(moduleName: String) {
        use(moduleName)

        localClasses.mergeWithModuleContents(modules.getValue(moduleName).load().unpack())
    }

    fun hide(className: String) {
        unqualifiedClasses.remove(className)
        localClasses.remove(className)
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

    operator fun ComponentClass.unaryPlus() {
        require(unqualifiedClasses[name]?.equals(this) != false) { "Cannot redeclare class $name." }
        localClasses[name] = this
        unqualifiedClasses[name] = this
    }


    internal fun build(): Module {
        return Module(name, moduleId, localClasses)
    }
}
