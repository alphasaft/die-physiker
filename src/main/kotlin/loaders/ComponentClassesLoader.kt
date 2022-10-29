package loaders

import Mapper
import Predicate
import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.BaseFunctionsRegister
import loaders.base.DataLoader
import physics.components.ComponentClass
import physics.components.ComponentBox
import physics.components.ComponentStructure
import physics.components.Field
import physics.quantities.PInt
import physics.quantities.PDouble
import physics.quantities.PString
import physics.quantities.PValue


class ComponentClassesLoader(
    loadedClasses: List<ComponentClass> = emptyList(),
    private val functionsRegister: FunctionsRegister,
) : DataLoader<ComponentClassesParser, Map<String, ComponentClass>>(ComponentClassesParser) {
    private val loadedClasses = loadedClasses.toMutableList()

    class FunctionsRegister internal constructor(): BaseFunctionsRegister {
        private val valueChecks = mutableMapOf<String, Predicate<PValue<*>>>()
        private val stringValuesNormalizers = mutableMapOf<String, Mapper<String>>()

        fun addValueCheck(checkRef: String, checkImpl: Predicate<PValue<*>>) {
            valueChecks[checkRef] = checkImpl
        }

        fun getValueCheck(checkRef: String): Predicate<PValue<*>> {
            return valueChecks[checkRef] ?: throw NoSuchElementException("Can't find value checker '$checkRef'.")
        }

        fun addStringValueNormalizer(normalizerRef: String, normalizerImpl: Mapper<String>) {
            stringValuesNormalizers[normalizerRef] = normalizerImpl
        }

        fun getStringValueNormalizer(normalizerRef: String): Mapper<String> {
            return stringValuesNormalizers[normalizerRef] ?: throw NoSuchElementException("Can't find string value normalizer '$normalizerRef'.")
        }
    }

    companion object {
        fun getFunctionsRegister() = FunctionsRegister()
    }

    private fun getClass(name: String) = loadedClasses
        .singleOrNull { it.name == name }
        ?: throw NoSuchElementException("Can't find class $name ; it maybe wasn't loaded.")

    override fun generateFrom(ast: Ast): Map<String, ComponentClass> {
        return ast
            .allNodes("component-#")
            .map { generateComponentClassFrom(it).also { cc -> loadedClasses.add(cc) } }
            .associateBy { it.name }
    }

    private fun generateComponentClassFrom(componentClassNode: AstNode): ComponentClass {
        val name = componentClassNode["name"]
        val abstract = componentClassNode.getOrNull("abstract") == "yes"
        val bases = (componentClassNode.getNodeOrNull("bases")?.allNodes("base-#") ?: emptyList()).map { getClass(it.content!!) }.toSet()
        val fields = componentClassNode.allNodes("field-#").associate { generateFieldTemplateFrom(it) }
        val subcomponentGroups = componentClassNode.allNodes("subcomponentGroup-#").associate { generateSubcomponentGroupTemplateFrom(it) }
        return ComponentClass(
            name,
            abstract,
            ComponentStructure(
                extends = bases,
                fieldsTemplates = fields,
                boxesTemplates = subcomponentGroups,
            )
        )
    }

    private fun generateFieldTemplateFrom(fieldNode: AstNode): Pair<String, Field.Template<*>> {
        val name = fieldNode["fieldName"]
        val notation = fieldNode.getOrNull("notation") ?: name
        val type = when (fieldNode["type"]) {
            "int", "integer" -> PInt::class
            "string" -> PString::class
            "double" -> PDouble::class
            else -> throw NoWhenBranchMatchedException()
        }
        return Pair(name, Field.Template(type, name, Field.Template.Notation.UseParenthesis(notation)))
    }

    private fun generateSubcomponentGroupTemplateFrom(groupNode: AstNode): Pair<String, ComponentBox.Template> {
        val name = groupNode["subcomponentGroupName"]
        val type = getClass(groupNode["subcomponentGroupType"])
        val sizeNode = groupNode.."size"
        val minimumSize = sizeNode["min"].toInt()
        val maximumSize = sizeNode["max"].toInt()
        return Pair(name, ComponentBox.Template(name, type, minimumSize, maximumSize))
    }
}