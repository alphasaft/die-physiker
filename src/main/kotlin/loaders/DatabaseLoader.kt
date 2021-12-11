package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.components.ComponentClass
import physics.computation.Database
import physics.computation.DatabaseOptions

class DatabaseLoader(private val loadedComponentClasses: Map<String, ComponentClass>) : DataLoader<DatabaseParser, Database>(DatabaseParser) {
    override fun generateFrom(ast: Ast): Database {
        val name = ast["name"]
        val fileName = ast["fileName"]
        val componentClass = loadedComponentClasses.getValue(ast["componentClass"])
        val links = generateLinksFrom(ast.."links")
        val options = generateOptionsFrom(ast.."options")

        return Database(
            name,
            options,
            from = fileName,
            given = componentClass,
            thenLink = links
        )
    }

    private fun generateLinksFrom(linksNode: AstNode): Map<String, String> {
        return linksNode.allNodes("link-#").associate { generateLinkFrom(it) }
    }

    private fun generateLinkFrom(linkNode: AstNode): Pair<String, String> {
        val fieldName = linkNode["fieldName"]
        val column = linkNode["column"]
        return fieldName to column
    }

    private fun generateOptionsFrom(optionsNode: AstNode): Int {
        return optionsNode.allNodes("option-#").map { generateOptionFrom(it) }.reduce(Int::or)
    }

    private fun generateOptionFrom(optionNode: AstNode): Int {
        return when (optionNode["optionName"]) {
            "case_insensitive" -> DatabaseOptions.CASE_INSENSITIVE
            "normalize" -> DatabaseOptions.NORMALIZE
            else -> throw NoSuchElementException("Option ${optionNode["optionName"]} doesn't exist")
        }
    }
}
