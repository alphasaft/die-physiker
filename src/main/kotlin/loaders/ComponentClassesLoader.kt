package loaders

import Mapper
import Predicate
import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.alwaysTrue
import physics.components.ComponentClass
import physics.components.ComponentGroup
import physics.components.Field
import physics.noop
import physics.values.*


class ComponentClassesLoader(
    private val valuesFactory: PhysicalValuesFactory,
    preloadedClasses: List<ComponentClass> = emptyList(),
    private val componentChecks: Map<String, Predicate<String>> = emptyMap(),
    private val valuesNormalizers: Map<String, Mapper<String>> = emptyMap(),
) : DataLoader<ComponentClassesParser, Map<String, ComponentClass>>(ComponentClassesParser) {

    private val loadedClasses = preloadedClasses.toMutableList()

    private fun getClass(name: String) = loadedClasses
        .singleOrNull { it.name == name }
        ?: throw NoSuchElementException("Can't find class $name ; it maybe wasn't loaded.")

    private fun getCheck(name: String) = componentChecks[name] ?: throw NoSuchElementException("Can't find check '$name'.")
    private fun getNormalizer(name: String) = valuesNormalizers[name] ?: throw NoSuchElementException("Can't find normalizer '$name'.")

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
            bases,
            fields,
            subcomponentGroups,
        )
    }

    private fun generateFieldTemplateFrom(fieldNode: AstNode): Field.Template<*> {
        val name = fieldNode["fieldName"]
        val notation = fieldNode.getOrNull("notation") ?: name
        val factory = when (fieldNode["type"]) {
            "double" -> valuesFactory.doubleFactoryWithUnit(fieldNode["unit"])
            "int" -> valuesFactory.intFactory()
            "string" -> generateStringFactoryFrom(fieldNode, valuesFactory)
            else -> throw NoWhenBranchMatchedException()
        }
        return Field.Template(name, notation, factory)
    }

    private fun generateStringFactoryFrom(fieldNode: AstNode, valuesFactory: PhysicalValuesFactory): PhysicalValue.Factory<PhysicalString> {
        val normalizer = fieldNode.getOrNull("normalizerFunctionRef")?.let { getNormalizer(it) } ?: ::noop
        val check = fieldNode.getOrNull("checkFunctionRef")?.let { getCheck(it) } ?: ::alwaysTrue
        return valuesFactory.stringFactory(normalizer, check)
    }

    private fun generateSubcomponentGroupTemplateFrom(groupNode: AstNode): ComponentGroup.Template {
        val name = groupNode["subcomponentGroupName"]
        val type = getClass(groupNode["subcomponentGroupType"])
        val sizeNode = groupNode.."size"
        val minimumSize = sizeNode["min"].toInt()
        val maximumSize = sizeNode["max"].toInt()
        return ComponentGroup.Template(name, type, minimumSize, maximumSize)
    }
}