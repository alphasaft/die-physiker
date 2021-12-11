package loaders.mpsi

import loaders.mpsi.statements.*
import loaders.mpsi.statements.ExternalDeclaration
import loaders.mpsi.statements.FunctionDeclaration
import loaders.mpsi.statements.ScopeDefiner


class Script {
    private val imports = mutableListOf<Import>()
    private val statements = mutableListOf<Statement>()

    internal fun addImport(import: Import) { imports.add(import) }
    internal fun addStatement(statement: Statement) { statements.add(statement) }

    private fun getExternalDeclarations() = statements.filterIsInstance<ExternalDeclaration>()
    private fun getScriptingStatements() = statements - getExternalDeclarations()

    override fun toString(): String {
        val buffer = StringBuffer()
        fun newline() { buffer.append("\n") }

        for (import in imports) buffer.append("$import\n")
        newline()

        for (externalDeclarations in getExternalDeclarations()) buffer.append("$externalDeclarations\n")
        newline()

        for (statement in getScriptingStatements()) {
            if (statement is FunctionDeclaration && buffer.takeLast(2) != "\n\n") newline()
            buffer.append("$statement\n")
            if (statement is ScopeDefiner || statement is FunctionDeclaration) newline()
        }

        return buffer.toString()
    }
}