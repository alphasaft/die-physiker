package loaders

import loaders.base.Parser


object FormulaParser : Parser() {
    override val whitespaces: String = " \n\t\r"
    private val identifierPossiblyComportingHash = Regex("[a-zA-Z_][a-zA-Z0-9_]*#?")

    override fun axiom() {
        oneOrMore("formula-#") {
            parseFormulaHeader()
            parseConcernsBlock()
            parseFormulaComputingBlock()
        }
    }

    private fun parseFormulaHeader() {
        consume("Formula")
        consume(string).trim('"')  [ "name" ]
        consume("for")
        consume(identifier)  [ "rootComponentType" ]
        consume(identifier)  [ "rootComponentName" ]
    }

    private fun parseConcernsBlock() {
        consume("concerns")

        optional {
            oneOrMore("componentSpec-#", ",") {
                parseComponentSpec()
            }
            consume(",")
        }

        oneOrMore("fieldSpec-#",",") {
            parseFieldSpec()
        }
    }
    private fun parseComponentSpec() {
        var name = consume(identifier)
        optional { name += consume("#") }
        name [ "name" ]
        consume("from")
        parseComponentDotField()
    }

    private fun parseFieldSpec() {
        parseComponentDotField("parentName", "storedIn")
    }

    private fun parseComponentDotField(componentStoredAs: String = "component", fieldStoredAs: String = "field") {
        consume(identifierPossiblyComportingHash) [ componentStoredAs ]
        consume(".")
        consume(identifier) [ fieldStoredAs ]
    }


    private fun parseFormulaComputingBlock() {
        consume("computing")
        oneOrMore("computingClause-#") {
            parseFormulaComputingClause()
        }
    }

    private fun parseFormulaComputingClause() {
        consumeRegex(identifier.pattern+"\\."+identifier.pattern)  [ "output" ]

        choice {
            option {
                "inline"  [ "type" ]

                consume("as")
                parseExpr()
            }

            option {
                "delegated"  [ "type" ]

                consume("by")
                consume(identifier)  [ "functionRef" ]
                consume(";")
            }
        }
    }

    private fun parseExpr() {
        consumeRegex(".*?;").removeSuffix(";").trim() [ "expr" ]
    }
}
