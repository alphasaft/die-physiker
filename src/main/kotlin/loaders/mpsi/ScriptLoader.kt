package loaders.mpsi

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import loaders.mpsi.statements.FunctionCall
import loaders.mpsi.statements.*
import loaders.mpsi.statements.ExternalVariableDeclaration
import loaders.mpsi.statements.Statement
import physics.components.ComponentClass
import physics.values.*
import physics.quantities.booleans.PBoolean
import physics.quantities.doubles.PReal
import physics.quantities.ints.PInt
import physics.quantities.strings.PString


class ScriptLoader(
    private val componentClasses: Map<String, ComponentClass>,
) : DataLoader<ScriptParser, Script>(ScriptParser) {

    private val builtinTypes = mapOf(
        "Boolean" to PBoolean::class,
        "Int" to PInt::class,
        "Double" to PReal::class,
        "String" to PString::class,
    )

    private val operatorsImplementations = mapOf(
        "+" to "plus",
        "-" to "minus",
        "*" to "times",
        "/" to "div",
        "**" to "pow",
        "==" to "equals",
        "!=" to "neq",
        "<" to "lt",
        "<=" to "le",
        ">" to "gt",
        ">=" to "ge",
        "||" to "or",
        "&&" to "and",
        "^" to "xor",
    )

    private val operatorsPrecedences = mapOf(
        "==" to 1,
        "!=" to 1,
        "<=" to 1,
        ">=" to 1,
        "<" to 1,
        ">" to 1,
        "||" to 2,
        "&&" to 2,
        "^" to 2,
        "+" to 3,
        "-" to 3,
        "*" to 4,
        "/" to 4,
        "**" to 5
    )

    override fun generateFrom(ast: Ast): Script {
        val script = Script()
        generateImports(ast.allNodes("import-#")).forEach { script.addImport(it) }
        generateExternalDeclarations(ast.allNodes("externalDeclaration-#")).forEach { script.addStatement(it) }
        generateStatements(ast.allNodes("statement-#")).forEach { script.addStatement(it) }
        return script
    }

    // --- Imports ---

    private fun generateImports(importsNodes: List<AstNode>): List<Import> {
        val imports = mutableListOf<Import>()
        for (importNode in importsNodes) {
            imports.add(Import(importNode["identifier"]))
        }
        return imports
    }

    // --- External declarations loading ---

    private fun generateExternalDeclarations(nodes: List<AstNode>): List<Statement> {
        val statements = mutableListOf<Statement>()
        for (externalDeclarationNode in nodes) {
            statements.add(when (externalDeclarationNode["type"]) {
                "externalVariableDeclaration" -> generateExternalVariableDeclaration(externalDeclarationNode)
                "externalFunctionDeclaration" -> generateExternalFunctionDeclaration(externalDeclarationNode)
                else -> throw NoWhenBranchMatchedException()
            })
        }
        return statements
    }

    private fun generateExternalVariableDeclaration(variableDeclarationNode: AstNode): ExternalVariableDeclaration {
        val name = variableDeclarationNode["varName"]
        val type = generateType(variableDeclarationNode.."varType")
        val loadedFrom = variableDeclarationNode.getOrNull("loadedFrom")
        return ExternalVariableDeclaration(name, type, loadedFrom)
    }

    // --- Statements ---

    private fun generateStatements(statementsNodes: List<AstNode>): List<Statement> {
        val statements = mutableListOf<Statement>()
        for (statementNode in statementsNodes) {
            generateStatement(statementNode)?.let { statements.add(it) }
        }
        return statements
    }

    private fun generateStatement(statementNode: AstNode): Statement? {
        return when (statementNode["type"]) {
            "comment" -> null
            "variableDeclaration" -> generateVariableDeclaration(statementNode)
            "variableAssignment" -> generateVariableAssignment(statementNode)
            "subscriptingSet" -> generateSubscriptingSetExpression(statementNode)
            "forLoop" -> generateForLoop(statementNode)
            "whileLoop" -> generateWhileLoop(statementNode)
            "ifElseBlock" -> generateIfElseStatement(statementNode)
            "expression" -> generateExpression(statementNode)
            "returnStatement" -> generateReturnStatement(statementNode)
            "functionDeclaration" -> generateFunctionDeclaration(statementNode)
            "componentModifier" -> generateComponentModifier(statementNode)
            "errorStatement" -> generateErrorStatement(statementNode)
            else -> throw NoWhenBranchMatchedException()
        }
    }

    private fun generateSubscriptingSetExpression(subscriptingNode: AstNode): FunctionCall {
        val variable = VariableAccess(subscriptingNode["variable"])
        val item = generateExpression(subscriptingNode.."item")
        val value = generateExpression(subscriptingNode.."value")
        return FunctionCall("set", listOf(variable, item, value))
    }

    private fun generateReturnStatement(returnStatementNode: AstNode): ReturnStatement {
        val expression = generateExpression(returnStatementNode.."expression")
        return ReturnStatement(expression)
    }

    private fun generateErrorStatement(errorStatementNode: AstNode): ErrorStatement {
        val message = errorStatementNode["message"]
        return ErrorStatement(message)
    }

    // --- Variables ---

    private fun generateVariableDeclaration(variableDeclarationNode: AstNode): VariableDeclaration {
        val varName = variableDeclarationNode["varName"]
        val varType = variableDeclarationNode.getNodeOrNull("varType")?.let { generateType(it) } ?: MpsiType.Infer
        val varValue = variableDeclarationNode.getNodeOrNull("varValue")?.let { generateExpression(it) }
        return VariableDeclaration(varName, varType, varValue)
    }

    private fun generateVariableAssignment(variableAssignmentNode: AstNode): VariableAssignment {
        val varName = variableAssignmentNode["varName"]
        val value = generateExpression(variableAssignmentNode.."varValue")
        return VariableAssignment(varName, value)
    }

    private fun generateVariableAccess(variableAccessNode: AstNode): VariableAccess {
        return VariableAccess(variableAccessNode["variableName"])
    }

    // --- Functions ---

    private fun generateExternalFunctionDeclaration(functionDeclarationNode: AstNode): ExternalFunctionDeclaration {
        val name = functionDeclarationNode["functionName"]
        val functionSignature = generateFunctionSignature(functionDeclarationNode.."functionSignature")
        return ExternalFunctionDeclaration(name, functionSignature)
    }

    private fun generateFunctionDeclaration(functionDeclarationNode: AstNode): FunctionDeclaration {
        val functionName = functionDeclarationNode["functionName"]
        val functionSignature = generateFunctionSignature(functionDeclarationNode.."functionSignature")
        val body = generateStatements((functionDeclarationNode.."functionBody").allNodes("statement-#"))
        return FunctionDeclaration(functionName, functionSignature, body)
    }

    private fun generateFunctionSignature(functionSignatureNode: AstNode): FunctionSignature {
        val parameters = generateFunctionParameters(functionSignatureNode.."parameters")
        val returnType = functionSignatureNode.getNodeOrNull("returnType")?.let { generateType(it) } ?: MpsiType.None
        return FunctionSignature(parameters, returnType)
    }

    private fun generateFunctionParameters(parametersNode: AstNode): Map<String, MpsiType> {
        return parametersNode.allNodes("parameter-#").associate { it["varName"] to generateType(it.."varType") }
    }

    private fun generateFunctionCall(functionCallNode: AstNode) : FunctionCall {
        val functionName = functionCallNode["functionName"]
        val arguments = generateFunctionArguments(functionCallNode.allNodes("argument-#"))
        return FunctionCall(functionName, arguments)
    }

    private fun generateFunctionArguments(argumentsNodes: List<AstNode>) : List<Expression> {
        return argumentsNodes.map { generateExpression(it) }
    }

    // --- Components ---

    private fun generateComponentBuilder(componentBuilderNode: AstNode): ComponentBuilderExpression {
        val className = componentBuilderNode["componentClass"]
        val body = componentBuilderNode.."componentBody"
        val fields = generateComponentFields(body)
        val groupsModifiers = generateGroupsModifiers(body)
        return ComponentBuilderExpression(
            componentClasses.getValue(className),
            fields,
            groupsModifiers
        )
    }

    private fun generateComponentModifier(componentModifierNode: AstNode): ComponentModifierStatement {
        val modifiedComponent = componentModifierNode["componentName"]
        val body = componentModifierNode.."componentBody"
        val fields = generateComponentFields(body)
        val groupsModifiers = generateGroupsModifiers(body)
        return ComponentModifierStatement(
            modifiedComponent,
            fields,
            groupsModifiers
        )
    }

    private fun generateComponentFields(componentBodyNode: AstNode): List<FieldSetter> {
        val fieldsSetters = mutableListOf<FieldSetter>()
        for (fieldNode in componentBodyNode.allNodes("field-#")) {
            val name = fieldNode["fieldName"]
            val value = generateExpression(fieldNode.."fieldValue")
            fieldsSetters.add(FieldSetter(name, value))
        }
        return fieldsSetters
    }

    private fun generateGroupsModifiers(componentBodyNode: AstNode): List<ComponentGroupModifier> {
        return componentBodyNode.allNodes("componentGroup-#").map { generateGroupModifier(it) }
    }

    private fun generateGroupModifier(groupNode: AstNode): ComponentGroupModifier {
        val name = groupNode["groupName"]
        val items = groupNode.allNodes("componentGroupItem-#")
        val addedComponents = items.filter { it["mode"] == "add" }.map { it["componentName"] }
        val removedComponents =  items.filter { it["mode"] == "remove" }.map { it["componentName"] }
        return ComponentGroupModifier(name, addedComponents, removedComponents)
    }

    // --- Control flow ---

    private fun generateForLoop(forLoopNode: AstNode): ForLoop {
        val header = forLoopNode.."forLoopHeader"
        val iteratingVariableName = header["iteratingVariableName"]
        val iteratingVariableType = header.getNodeOrNull("iteratingVariableType")?.let { generateType(it) } ?: MpsiType.Infer
        val iteratingVariable = iteratingVariableName to iteratingVariableType
        val iteratedValue = generateExpression(header.."iteratedValue")
        val body = generateStatements((forLoopNode.."forLoopBody").allNodes("statement-#"))
        return ForLoop(iteratingVariable, iteratedValue, body)
    }

    private fun generateWhileLoop(whileLoopNode: AstNode): WhileLoop {
        val condition = generateExpression(whileLoopNode.."condition")
        val body = generateStatements((whileLoopNode.."whileLoopBody").allNodes("statement-#"))
        return WhileLoop(condition, body)
    }

    private fun generateIfElseStatement(ifElseNode: AstNode): IfElseStatement {
        val condition = generateExpression(ifElseNode.."condition")
        val ifBlock = generateStatements((ifElseNode.."ifBlock").allNodes("statement-#"))
        val elseBlock = ifElseNode.getNodeOrNull("elseBlock")?.let { generateStatements(it.allNodes("statement-#")) }
        return IfElseStatement(condition, ifBlock, elseBlock)
    }

    // --- Atoms ---

    private fun generateExpression(expressionNode: AstNode): Expression {
        return when (expressionNode["expressionType"]) {
            "componentBuilder" -> generateComponentBuilder(expressionNode)
            "arithmeticalExpression" -> generateArithmeticalExpression(expressionNode)
            else -> throw NoWhenBranchMatchedException()
        }
    }

    private fun generateArithmeticalExpression(expressionNode: AstNode): Expression {
        val operands = expressionNode.allNodes("operand-#")
        val operators = expressionNode.allNodes("operator-#")
        return generateExpressionFromOperatorsAndOperands(operands, operators.map { it.content!! })
    }

    private fun generateExpressionFromOperatorsAndOperands(operands: List<AstNode>, operators: List<String>): Expression {
        if (operands.size == 1) return generateOperand(operands.single())

        val lowestPriorityOperatorIndex = getLowestPriorityOperatorIndex(operators)
        val lowestPriorityOperator = operators[lowestPriorityOperatorIndex]
        val operatorImplementationName = operatorsImplementations.getValue(lowestPriorityOperator)

        val leftOperands = operands.take(lowestPriorityOperatorIndex+1)
        val rightOperands = operands.drop(lowestPriorityOperatorIndex+1)
        val leftOperators = operators.take(lowestPriorityOperatorIndex)
        val rightOperators = operators.drop(lowestPriorityOperatorIndex+1)
        val left = generateExpressionFromOperatorsAndOperands(leftOperands, leftOperators)
        val right = generateExpressionFromOperatorsAndOperands(rightOperands, rightOperators)

        return FunctionCall(operatorImplementationName, listOf(left, right))
    }

    private fun getLowestPriorityOperatorIndex(operators: List<String>): Int =
        operators.withIndex().minByOrNull { (_, operator) -> operatorsPrecedences.getValue(operator) }!!.index

    private fun generateOperand(operandNode: AstNode): Expression {
        return when (operandNode["type"]) {
            "functionCall" -> generateFunctionCall(operandNode)
            "subscriptingGet" -> generateSubscriptingGetExpression(operandNode)
            "variableAccess" -> generateVariableAccess(operandNode)
            "literal" -> generateLiteral(operandNode)
            "expression" -> generateExpression(operandNode.."expression")
            else -> throw NoWhenBranchMatchedException()
        }
    }

    private fun generateSubscriptingGetExpression(subscriptingNode: AstNode) : FunctionCall {
        return FunctionCall("get", listOf(
            VariableAccess(subscriptingNode["variable"]),
            generateExpression(subscriptingNode.."item")
        ))
    }

    private fun generateLiteral(literalNode: AstNode) : Expression {
        val nodeContent = literalNode.content!!

        return when (literalNode["literalType"]) {
            "list" -> generateList(literalNode)
            "tuple" -> generateTuple(literalNode)
            "map" -> generateMap(literalNode)
            "boolean" -> MpsiBuiltinLiteral(PBoolean(nodeContent == "true"))
            "int" -> MpsiBuiltinLiteral(PInt(nodeContent.toInt()))
            "double" -> MpsiBuiltinLiteral(PReal(nodeContent.toDouble()))
            "string" -> MpsiBuiltinLiteral(PString(nodeContent.trim('\"')))
            "null" -> Null
            else -> throw NoWhenBranchMatchedException()
        }
    }

    private fun generateList(listNode: AstNode) : MpsiListLiteral {
        return MpsiListLiteral(listNode.allNodes("item-#").map { generateExpression(it) })
    }

    private fun generateTuple(tupleNode: AstNode) : MpsiTupleLiteral {
        return MpsiTupleLiteral(tupleNode.allNodes("item-#").map { generateExpression(it) })
    }

    private fun generateMap(mapNode: AstNode) : MpsiMapLiteral {
        return MpsiMapLiteral(mapNode.allNodes("pair-#").associate { generateExpression(it.."key") to generateExpression(it.."value") })
    }

    // --- Types ---

    private fun generateType(typeNode: AstNode): MpsiType {
        return when (typeNode["category"]) {
            "builtin" -> generateBuiltinType(typeNode)
            "component" -> generateComponentType(typeNode)
            "tuple" -> generateTupleType(typeNode)
            "list" -> generateListType(typeNode)
            "map" -> generateMapType(typeNode)
            "nullable" -> generateNullableType(typeNode)
            else -> throw NoWhenBranchMatchedException()
        }
    }

    private fun generateBuiltinType(typeNode: AstNode): MpsiType.Builtin {
        return MpsiType.Builtin(builtinTypes.getValue(typeNode["type"]))
    }

    private fun generateComponentType(typeNode: AstNode): MpsiType.Component {
        return MpsiType.Component(typeNode["type"])
    }

    private fun generateTupleType(typeNode: AstNode): MpsiType.Tuple {
        val itemTypes = mutableListOf<MpsiType>()
        for (itemTypeNode in typeNode.allNodes("itemType-#")) itemTypes.add(generateType(itemTypeNode))
        return MpsiType.Tuple(itemTypes)
    }

    private fun generateListType(typeNode: AstNode): MpsiType.List {
        val itemType = generateType(typeNode.."itemType")
        return MpsiType.List(itemType)
    }

    private fun generateMapType(typeNode: AstNode): MpsiType.Map {
        val keyType = MpsiType.Builtin(builtinTypes.getValue((typeNode.."keyType").content!!))
        val valueType = generateType(typeNode.."valueType")
        return MpsiType.Map(keyType, valueType)
    }

    private fun generateNullableType(typeNode: AstNode): MpsiType.Nullable {
        val underlyingType = generateType(typeNode.."underlyingType")
        return MpsiType.Nullable(underlyingType)
    }
}