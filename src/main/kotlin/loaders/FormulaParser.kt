package loaders

import loaders.base.Parser


object FormulaParser : Parser() {
    override fun axiom() {
        header()
        consume("\n")
        specs()
        consume("\n")
        output()
        consume("\n")
        equality()
        optional {
            consume("\n")
            adaptableVariables()
        }
    }

    private fun header() {
        consumeSentence("La formule")
        optional { consume("implicite") ; "yes" [ "implicit" ] }
        string().trim('"')  [ "name" ]
    }

    private fun specs() {
        consumeSentence("concerne : \n")
        node("specs") {
            oneOrMore("spec-#", separator = "\n") {
                consume("-")
                invokeAsSubParser(RequirementParser)
            }
        }
    }

    private fun output() {
        consumeSentence("renvoie : \n")
        node("output") {
            consumeRegex("$identifier.[$letter ]+").trim()  [ "location" ]
            consumeSentence("( '")
            identifier()  [ "variableName" ]
            consumeSentence("' )")
        }
    }

    private fun equality() {
        consumeSentence("on a : \n")

        node("equality") {
            node("left") {
                expression()
            }

            consume("=")

            node("right") {
                expression()
            }
        }
    }

    private fun expression() {
        node("operand-1") { operand() }

        var i = 2
        zeroOrMore {
            node("operator-${i-1}") { operator() }
            node("operand-$i") { operand() }
            i++
        }
    }

    private fun operand() {
        var type: String? = null

        choice {
            option {
                multiVariablesCollector()
                type = "multiVariablesCollector"
            }
            option {
                consumeRegex("[a-zA-Z_#][a-zA-Z0-9_#]*") [ "variableName" ]
                type = "variable"
            }
            option {
                double()  [ "value" ]
                type = "double"
            }
            option {
                integer()  [ "value" ]
                type = "integer"
            }
            option {
                consume("(")
                node("subexpression") { expression() }
                consume(")")
                type = "expression"
            }
        }

        type!!  [ "type" ]
    }

    private fun multiVariablesCollector() {
        consume("(")
        node("genericExpression") { expression() }
        consume("}")
        identifier()  [ "collector" ]
    }

    private fun operator() {
        consumeRegex("([\\-+*/]|\\*\\*)")
    }

    private fun adaptableVariables() {
        consumeSentence("variables adaptables : \n")
        node("adaptableVariables") {
            oneOrMore("adaptableVariable-#", ",") { identifier() }
        }
    }
}