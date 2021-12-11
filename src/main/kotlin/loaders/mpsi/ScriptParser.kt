package loaders.mpsi

import loaders.base.DefinesScope
import loaders.base.Parser


// Micro-Scripts and Programs Implementation
object ScriptParser : Parser() {
    override val nl = "(;|\n+)"
    
    private val allStatements = listOf(
        ::comment,
        ::variableDeclaration,
        ::variableAssignment,
        ::subscriptingSet,
        ::forLoop,
        ::whileLoop,
        ::ifStatement,
        ::returnStatement,
        ::functionDeclaration,
        ::componentBuilder,
        ::componentModifier,
        ::errorStatement,
        ::expression,
    )

    override fun axiom() {
        imports()
        nl()
        externalDeclarations()
        nlOpt()
        statements()
    }

    // --- Imports ---

    private fun imports() {
        oneOrMore("import-#", nl) {
            import()
        }
    }

    private fun import() {
        consume("import")
        identifier()  [ "identifier" ]
    }

    // --- 'Expect' statements ---

    private fun externalDeclarations() {
        zeroOrMore("externalDeclaration-#", "\n+") {
            choice {
                option {
                    externalVariableDeclaration()
                    "type".."externalVariableDeclaration"
                }
                option {
                    externalFunctionDeclaration()
                    "type".."externalFunctionDeclaration"
                }
            }
        }
    }

    private fun externalVariableDeclaration() {
        consume("expect")
        consume("var")
        identifier()  [ "varName" ]
        consume(":")
        node("varType", ::anyType)
        optional {
            consume("from")
            string()  [ "loadedFrom" ]
        }
    }

    private fun externalFunctionDeclaration() {
        consume("expect")
        functionHeader()
    }

    // --- Statements ---

    private fun statements() {
        zeroOrMore("statement-#", "(\n+|;)") {
            negativeLookahead("expect", "'expect' : External declarations should be done before statements.")
            statement()
        }
    }

    private fun statement() {
        choice {
            for (statementBlock in allStatements) option {
                statementBlock()
                "type"..statementBlock.name
            }
        }
    }

    private fun returnStatement() {
        consume("return")
        node("expression", ::expression)
    }

    private fun errorStatement() {
        consume("error")
        string()  [ "message" ]
    }

    private fun comment() {
        consumeRegex("//.*?(?=\n)")
    }

    // --- Control flow ---

    private fun controlFlowStatement(headerName: String, header: () -> Unit, bodyName: String, footer: () -> Unit = {}) {
        node(headerName, header)
        consume("{")
        nlOpt()
        node(bodyName, ::statements)
        nlOpt()
        consume("}")
        footer()
    }

    private fun forLoopHeader() {
        consume("for")
        consume("(")
        identifier()  [ "iteratingVariableName" ]
        optional {
            consume(":")
            node("iteratingVariableType", ::anyType)
        }
        consume("in")
        node("iteratedValue", ::expression)
        consume(")")
    }

    private fun forLoop() {
        controlFlowStatement(headerName = "forLoopHeader", header = ::forLoopHeader, bodyName = "forLoopBody")
    }

    private fun whileLoopHeader() {
        consume("while")
        consume("(")
        expression()
        consume(")")
    }

    private fun whileLoop() {
        controlFlowStatement(headerName = "condition", header = ::whileLoopHeader, bodyName = "whileLoopBody")
    }

    private fun elseBlock() {
        node("elseBlock") {
            controlFlowStatement(
                headerName = "else",
                header = { consume("else") },
                bodyName = "elseBlockBody"
            )
        }
    }

    private fun ifStatementHeader() {
        consume("if")
        consume("(")
        expression()
        consume(")")
    }

    private fun ifStatement() {
        controlFlowStatement(
            headerName = "condition",
            header = ::ifStatementHeader,
            bodyName = "ifBlock",
            footer = { optional { elseBlock() } }
        )
    }

    // --- Functions ---

    private fun functionDeclaration() {
        functionHeader()
        consumeSentence("{")
        node("functionBody") {
            nl()
            statements()
            nl()
        }
        consumeSentence("}")
    }

    private fun functionHeader() {
        consume("fun")
        identifier() [ "functionName" ]
        functionSignature()
    }

    @DefinesScope
    private fun functionSignature() {
        node("functionSignature") {
            consume("(")
            node("parameters") { mapPattern(",", ":", "parameter-#", "varName" to { identifier() }, "varType" to ::anyType) }
            consume(")")
            optional {
                consume(":")
                node("returnType") { anyType() }
            }
        }
    }

    // --- Components ---

    private fun componentBuilder() {
        consume("create")
        identifier()  [ "componentClass" ]
        node("componentBody") {
            optional {
                consume("{")
                nlOpt()
                componentBody(modificationAllowed = false)
                nlOpt()
                consume("}")
            }
        }
    }

    private fun componentModifier() {
        consume("modify")
        identifier()  [ "componentName" ]
        consume("{")
        node("componentBody") {
            nlOpt()
            componentBody(modificationAllowed = true)
            nlOpt()
        }
        consume("}")
    }

    private fun componentBody(modificationAllowed: Boolean) {
        var i = 1
        var j = 1
        zeroOrMore(separator = nl) {
            choice {
                option("field-$i") { field() ; i++ }
                option("componentGroup-$j") { componentGroup(modificationAllowed = modificationAllowed) ; j++ }
            }
        }
    }

    private fun field() {
        consume("field")
        choice {
            option { identifier()  [ "fieldName" ] }
            option { string().trim()  [ "fieldName" ] }
        }
        consume("=")
        node("fieldValue", ::expression)
    }

    private fun componentGroup(modificationAllowed: Boolean) {
        consume("group")
        identifier()  [ "groupName" ]
        consume("{")
        nlOpt()
        oneOrMore("componentGroupItem-#", nl) {
            componentGroupItem(modificationAllowed = modificationAllowed)
        }
        nlOpt()
        consume("}")
    }

    private fun componentGroupItem(modificationAllowed: Boolean) {
        if (!modificationAllowed) {
            negativeLookahead("-", "'-' : Component is being created, not modified, thus '-' isn't legal here.")
            consume("+"); "mode".."add"
        } else {
            choice {
                option { consume("+"); "mode".."add" }
                option { consume("-"); "mode".."remove" }
            }
        }
        identifier()  [ "componentName" ]
    }

    // --- Variables ---

    private fun variableDeclaration() {
        consume("var")
        identifier()  [ "varName" ]
        choice {
            option {
                consume(":")
                node("varType", ::anyType)
                consume("=")
                node("varValue") { expression() }
            }
            option {
                choice {
                    option {
                        consume(":")
                        node("varType", ::anyType)
                    }
                    option {
                        consume("=")
                        node("varValue") { expression() }
                    }
                }
            }
        }
    }
    
    private fun variableAssignment() {
        identifier()  [ "varName" ]
        consume("=")
        node("varValue")  { expression() }
    }


    // --- Atoms ---

    private fun expression() {
        choice {
            option { componentBuilder() ; "expressionType".."componentBuilder" }
            option { arithmeticalExpression() ; "expressionType".."arithmeticalExpression" }
        }
    }

    private fun arithmeticalExpression() {
        var i = 1
        node("operand-${i++}") { operand() }
        zeroOrMore {
            consumeRegex("(\\+|-|\\*|/|\\*\\*|==|<|>|!=|<=|>=|\\|\\||&&)")  [ "operator-${i-1}" ]
            node("operand-${i++}") { operand() }
        }
        negativeLookahead("[", "'[' : Indexing operator can only be used on variables, not on expressions.")
    }

    private fun operand() {
        choice {
            option { functionCall() ; "type".."functionCall" }
            option { subscriptingGet() ; "type".."subscriptingGet" }
            option { identifier()  [ "variableName" ] ; "type".."variableAccess" }
            option { literal() ; "type".."literal" }
            option { consume("(") ; node("expression", ::expression) ; consume(")") ; "type".."expression" }
        }
    }

    private fun subscriptingGet() {
        identifier() [ "variable" ]
        consume("[")
        node("item", ::expression)
        consume("]")
    }

    private fun subscriptingSet() {
        subscriptingGet()
        consume("=")
        node("value", ::expression)
    }

    private fun functionCall() {
        identifier()  [ "functionName" ]
        consume("(")
        collectionPattern(",", "argument-#", ::expression)
        consume(")")
    }

    private fun literal() {
        choice {
            option { builtinLiteral() }
            option { listLiteral() ; "literalType" .. "list" }
            option { tupleLiteral() ; "literalType" .. "tuple" }
            option { mapLiteral() ; "literalType" .. "map" }
            option { consume("null") ; "literalType".."null" }
        }
    }

    private fun builtinLiteral() {
        choice {
            option { consume("(true|false)") ; "literalType".."boolean" }
            option { double() ; "literalType" .. "double" }
            option { integer() ; "literalType" .. "int" }
            option { string() ; "literalType" .. "string" }
        }
    }

    private fun listLiteral() {
        consume("[")
        collectionPattern(",", "item-#", ::expression)
        consume("]")
    }

    private fun tupleLiteral() {
        consume("((")
        collectionPattern(",", "item-#", ::expression)
        consume("))")
    }

    private fun mapLiteral() {
        consume("{")
        mapPattern(",", ":", "pair-#", "key" to ::expression, "value" to ::expression)
        consume("}")
    }

    // --- Types ---

    private fun anyType() {
        choice {
            option { nullableType() ; "category".."nullable" }
            option { builtinPhysicalType() [ "type" ] ; "category".."builtin" }
            option { componentType() [ "type" ] ; "category".."component" }
            option { tupleOfPhysicalTypes() ; "category".."tuple" }
            option { physicalListType() ; "category".."list" }
            option { physicalMapType() ; "category".."map" }
        }
    }

    private fun nullableType() {
        consume("?")
        node("underlyingType", ::anyType)
    }

    private fun builtinPhysicalType(): String {
        return consumeRegex("(Boolean|Int|Double|String)")
    }

    private fun componentType(): String {
        return identifier()
    }

    private fun tupleOfPhysicalTypes() {
        consume("(")
        collectionPattern(",", "itemType-#", ::anyType)
        consume(")")
    }

    private fun physicalListType() {
        consume("[")
        node("itemType") { anyType() }
        consume("]")
    }

    private fun physicalMapType() {
        consume("{")
        node("keyType") { builtinPhysicalType() }
        consume(":")
        node("valueType") { anyType() }
        consume("}")
    }
}