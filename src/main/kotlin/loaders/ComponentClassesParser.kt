package loaders

import loaders.base.Parser

object ComponentClassesParser : Parser() {
    override fun axiom() {
        oneOrMore("component-#", separator = "\n+") {
            component()
        }
    }

    private fun component() {
        header()
        optional {
            consume("\n")
            fields()
        }
        optional {
            consume("\n")
            subcomponents()
        }
    }

    private fun header() {
        consumeSentence("Le composant")
        optional { consume("abstrait") ; "yes"  [ "abstract" ] }
        consumeRegex(identifier)  [ "name" ]
        optional { extendsBlock() }
    }

    private fun extendsBlock() {
        consume("étendant")
        group("bases") {
            oneOrMore("base-#", separator = ",") {
                consumeRegex(identifier)
            }
        }
    }

    private fun fields() {
        consumeSentence("possède : \n")
        oneOrMore("field-#", separator = "\n") {
            consume("-")
            consumeRegex("(une|un|des)")
            consumeRegex(identifier)  [ "fieldName" ]
            consume("(")
            fieldType()
            consume(")")
            optional {
                notation()
            }
        }
    }

    private fun fieldType() {
        var type: String? = null
        choice {
            option {
                consume("Double")
                consume(":")
                consumeRegex(identifier)  [ "unit" ]
                type = "double"
            }
            option {
                consumeRegex("Int(eger)?")
                type = "int"
            }
            option {
                consume("String")
                optional {
                    consume("<")
                    consumeRegex(identifier)  [ "checkFunctionRef" ]
                    consume(">")
                }
                optional {
                    consume("->")
                    consumeRegex(identifier)  [ "normalizerFunctionRef" ]
                }
                type = "string"
            }
        }
        type!!  [ "type" ]
    }

    private fun notation() {
        consume("noté")
        choice {
            option { consumeRegex(identifier)  [ "notation" ] }
            option { consumeRegex(string).trim('"') [ "notation" ] }
        }
    }

    private fun subcomponents() {
        consumeSentence("sous-composants : \n")
        oneOrMore("subcomponentGroup-#", separator = "\n") {
            consume("-")
            group("size") { componentGroupSize() }
            consumeRegex(identifier)  [  "subcomponentGroupName"  ]
            consume("(")
            consumeRegex(identifier)  [  "subcomponentGroupType"  ]
            consume(")")
        }
    }

    private fun componentGroupSize() {
        var min: String? = null
        var max: String? = null

        choice {
            option {
                val pMin = consumeRegex(integer)
                consume("-")
                val pMax = consumeRegex(integer)
                min = pMin
                max = pMax
            }
            option {
                val pMax = consumeRegex(integer)
                consume("-")
                min = "0"
                max = pMax
            }
            option {
                val pMin = consumeRegex(integer)
                consume("+")
                min = pMin
                max = "-1"
            }
            option {
                val size = consumeRegex(integer)
                min = size
                max = size
            }
        }

        min!!  [ "min" ]
        max!!  [ "max" ]
    }
}