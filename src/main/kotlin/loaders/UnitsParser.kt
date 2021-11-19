package loaders

import loaders.base.Parser

object UnitsParser : Parser() {
    // TODO : Rewrite the parser.
    override fun axiom() {
        oneOrMore("unitGroup-#", separator = "\n+") {
            header()
            allUnits()
        }
    }

    private fun header() {
        consumeRegex("(Les|Le|La)")
        consumeRegex(identifier)  [  "measuredQuantity"  ]
        consumeSentence("s ' exprime")
        optional { consume("nt") }
        consumeSentence("en")
    }

    private fun allUnits() {
        group("mainUnit") {
            mainUnit()
        }
        group("units") {
            zeroOrMore("unit-#") {
                consume("/")
                unit()
            }
        }
    }

    private fun mainUnit() {
        consumeRegex(identifier)  [ "symbol" ]
        optional {
            consumeSentence("( <=>")
            group("associatedSignature") {
                unitSignature()
            }
            consume(")")
        }
    }

    private fun unit() {
        consumeRegex(identifier)  [ "symbol" ]
        unitSpec()
    }

    private fun unitSpec() {
        consume("(")
        group("converter") {
            consumeRegex("($double|$integer)")  [ "coefficient" ]
            consumeRegex(identifier)  [ "target" ]
        }
        consume(")")
    }

    private fun unitSignature() {
        oneOrMore("signaturePart-#", separator = ".") {
            consumeRegex("[a-zA-Z]+")  [ "unit" ]
            var exponent = "1"
            optional {
                exponent = consumeRegex(integer)
            }
            exponent  [ "exponent" ]
        }
    }
}
