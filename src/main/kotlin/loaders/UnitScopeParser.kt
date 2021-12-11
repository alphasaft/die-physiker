package loaders

import loaders.base.Parser

object UnitScopeParser : Parser() {
    // TODO : Rewrite the parser.
    override fun axiom() {
        oneOrMore("unitGroup-#", separator = "\n+") {
            header()
            allUnits()
        }
    }

    private fun header() {
        consumeRegex("(Les|Le|La)")
        choice {
            option {
                identifier()  [  "measuredQuantity"  ]
            }
            option {
                string().trim('"')  [ "measuredQuantity" ]
            }
        }
        consumeSentence("s ' exprime")
        optional { consume("nt") }
        consumeSentence("en")
    }

    private fun allUnits() {
        node("mainUnit") {
            mainUnit()
        }
        node("units") {
            zeroOrMore("unit-#") {
                consume("/")
                unit()
            }
        }
    }

    private fun mainUnit() {
        identifier()  [ "symbol" ]
        optional {
            consumeSentence("( <=>")
            node("associatedSignature") {
                unitSignature()
            }
            consume(")")
        }
    }

    private fun unit() {
        identifier()  [ "symbol" ]
        unitSpec()
    }

    private fun unitSpec() {
        consume("(")
        node("converter") {
            consumeRegex("($double|$integer)")  [ "coefficient" ]
            identifier()  [ "target" ]
        }
        consume(")")
    }

    private fun unitSignature() {
        invokeAsSubParser(UnitSignatureParser)
    }
}
