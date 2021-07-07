package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.PhysicalComponentModel
import physics.RootPhysicalComponentModel
import physics.specs.ComponentSpec
import physics.specs.FieldSpec
import physics.specs.ProxySpec


object ComponentModelsLoader : DataLoader<ComponentModelParser, Map<String, PhysicalComponentModel>>(ComponentModelParser) {
    override fun generateFrom(ast: Ast): Map<String, PhysicalComponentModel> {
        return ast.allNodes("component-#").associate(::generatePhysicalComponentModel)
    }

    private fun generatePhysicalComponentModel(node: AstNode): Pair<String, PhysicalComponentModel> {
        val ctr = if (node["isRoot"] == "yes") ::RootPhysicalComponentModel else ::PhysicalComponentModel
        val name = node["name"]
        val fieldsSpecs = generateFieldsSpecs(node)
        val proxiesSpecs = generateProxiesSpecs(node)
        val subComponentsSpecs = generateSubComponentsSpecs(node)
        return name to ctr(name, fieldsSpecs, proxiesSpecs, subComponentsSpecs)
    }

    private fun generateFieldsSpecs(node: AstNode): List<FieldSpec> {
        val specs = mutableListOf<FieldSpec>()
        for (specNode in node.allNodes("field-#")) {
            specs.add(FieldSpec(specNode["name"], specNode["type"]))
        }
        return specs
    }

    private fun generateProxiesSpecs(node: AstNode): List<ProxySpec> {
        val proxies = mutableListOf<ProxySpec>()
        for (proxyNode in node.allNodes("proxy-#")) {
            proxies.add(ProxySpec(proxyNode["name"], proxyNode["target"]))
        }
        return proxies
    }

    private fun generateSubComponentsSpecs(node: AstNode): List<ComponentSpec> {
        val specs = mutableListOf<ComponentSpec>()
        for (specNode in node.allNodes("subcomponent-#")) {
            specs.add(ComponentSpec(specNode["name"], specNode["type"], specNode["atLeast"].toInt(), specNode["atMost"].toInt()))
        }
        return specs
    }
}
