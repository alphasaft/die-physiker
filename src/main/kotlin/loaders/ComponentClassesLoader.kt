package loaders

import Mapper
import Predicate
import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.BaseFunctionsRegister
import loaders.base.DataLoader
import physics.alwaysTrue
import physics.components.ComponentClass
import physics.components.ComponentGroup
import physics.components.ComponentStructure
import physics.components.Field
import physics.noop
import physics.values.*


class ComponentClassesLoader(
    private val valuesFactory: PhysicalValuesFactory,
    preloadedClasses: List<ComponentClass> = emptyList(),
    private val functionsRegister: ComponentClassesLoader.FunctionsRegister,
) : DataLoader<ComponentClassesParser, Map<String, ComponentClass>>(ComponentClassesParser) {
    private val loadedClasses = preloadedClasses.toMutableList()

    class FunctionsRegister internal constructor(): BaseFunctionsRegister {
        private val stringValuesChecks = mutableMapOf<String, Predicate<String>>()
        private val stringValuesNormalizers = mutableMapOf<String, Mapper<String>>()

        fun addStringValueCheck(checkerRef: String, checkerImpl: Predicate<String>) {
            stringValuesChecks[checkerRef] = checkerImpl
        }

        fun getStringValueCheck(checkerRef: String): Predicate<String> {
            return stringValuesChecks[checkerRef] ?: throw NoSuchElementException("Can't find string value checker '$checkerRef'.")
        }

        fun addStringValueNormalizer(normalizerRef: String, normalizerImpl: Mapper<String>) {
            stringValuesNormalizers[normalizerRef] = normalizerImpl
        }

        fun getStringValueNormalizer(normalizerRef: String): Mapper<String> {
            return stringValuesNormalizers[normalizerRef] ?: throw NoSuchElementException("Can't find normalizer string value normalizer '$normalizerRef'.")
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
        val representationField = componentClassNode.getOrNull("representationField")
        return ComponentClass(
            name,
            abstract,
            ComponentStructure(
                extends = bases,
                fieldsTemplates = fields,
                subcomponentsGroupsTemplates = subcomponentGroups,
            ),
            representationField
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
        val normalizer = fieldNode.getOrNull("normalizerFunctionRef")?.let { functionsRegister.getStringValueNormalizer(it) } ?: ::noop
        val check = fieldNode.getOrNull("checkFunctionRef")?.let { functionsRegister.getStringValueCheck(it) } ?: ::alwaysTrue
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