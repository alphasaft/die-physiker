package loaders

import loaders.base.Parser


object FormulaExpressionParser : Parser() {
    override fun axiom() {
        expr()
    }

    fun expr() {
        var operandIndex = 1
        var operatorIndex = 1

        operand(operandIndex++)
        zeroOrMore {
            operator(operatorIndex++)
            operand(operandIndex++)
        }
    }

    fun operand(index: Int) {
        choice("operand-$index") {
            option {
                "collector"  [ "type" ]
                collectionCollector()
            }

            option {
                "variable" [ "type" ]
                consumeRegex("[a-zA-Z_][a-zA-Z0-9_#]*\\."+identifier.pattern)  [ "value" ]
            }

            option {
                "function"  [ "type" ]

                consume(identifier)  [ "functionName" ]
                consume("(")
                group("value") {
                    expr()
                }
                consume(")")
            }

            option {
                "constant"  [ "type" ]
                consumeRegex(identifier)  [ "constName" ]
            }

            option {
                "float"  [ "type" ]
                consume(float) [ "value" ]
            }

            option {
                "float" [ "type" ]
                (consume(integer) + ".0")  [ "value" ]
            }

            option {
                "subExpr"  [ "type" ]

                consume("(")
                group("value") {
                    expr()
                }
                consume(")")
            }
        }
    }

    private fun collectionCollector() {
        consumeRegex("$identifier#\\."+identifier.pattern)  [ "collection" ]
        consume("->")
        consumeRegex(identifier)  [ "collector" ]
    }

    private fun operator(index: Int) {
        consumeRegex("[-+/^*]")  [ "operator-$index" ]
    }
}