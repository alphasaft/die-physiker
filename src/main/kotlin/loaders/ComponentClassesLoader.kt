package loaders

import noop
import Mapper
import Predicate
import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.BaseFunctionsRegister
import loaders.base.DataLoader
import physics.components.ComponentClass
import physics.components.Group
import physics.components.ComponentStructure
import physics.components.Field
import physics.values.*
import physics.quantities.PValue
import physics.quantities.strings.PString


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
        val bases = (componentClassNode.getNodeOrNull("bases")?.allNodes("base-#") ?: emptyList()).map { getClass(it.content!!) }
        val fields = componentClassNode.allNodes("field-#").map { generateFieldTemplateFrom(it) }
        val subcomponentGroups = componentClassNode.allNodes("subcomponentGroup-#").map { generateSubcomponentGroupTemplateFrom(it) }
        return ComponentClass(
            name,
            abstract,
            ComponentStructure(
                extends = bases,
                fieldsTemplates = fields,
                subcomponentsGroupsTemplates = subcomponentGroups,
            )
        )
    }

    private fun generateFieldTemplateFrom(fieldNode: AstNode): Field.Template<*> {
        val name = fieldNode["fieldName"]
        val notation = fieldNode.getOrNull("notation") ?: name
        TODO()
    }

    private fun generateSubcomponentGroupTemplateFrom(groupNode: AstNode): Group.Template {
        val name = groupNode["subcomponentGroupName"]
        val type = getClass(groupNode["subcomponentGroupType"])
        val sizeNode = groupNode.."size"
        val minimumSize = sizeNode["min"].toInt()
        val maximumSize = sizeNode["max"].toInt()
        return Group.Template(name, type, minimumSize, maximumSize)
    }
}