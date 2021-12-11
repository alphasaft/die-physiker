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
            all(separator = "\n") {
                optionalBlock {
                    fields()
                }
                optionalBlock {
                    subcomponents()
                }
                optionalBlock {
                    representation()
                }
            }
        }
    }

    private fun header() {
        consumeSentence("Le composant")
        optional { consume("abstrait") ; "yes"  [ "abstract" ] }
        identifier()  [ "name" ]
        optional { extendsBlock() }
    }

    private fun extendsBlock() {
        consume("étendant")
        node("bases") {
            oneOrMore("base-#", separator = ",") {
                identifier()
            }
        }
    }

    private fun fields() {
        consumeSentence("possède : \n")
        oneOrMore("field-#", separator = "\n") {
            consume("-")
            consumeRegex("une|un|des")
            words().trim()  [ "fieldName" ]
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
                invokeAsSubParser(UnitSignatureParser, "unit")
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
                    identifier()  [ "checkFunctionRef" ]
                    consume(">")
                }
                optional {
                    consume("->")
                    identifier()  [ "normalizerFunctionRef" ]
                }
                type = "string"
            }
        }
        type!!  [ "type" ]
    }

    private fun notation() {
        consume("noté")
        choice {
            option { identifier()  [ "notation" ] }
            option { string().trim('"') [ "notation" ] }
        }
    }

    private fun subcomponents() {
        consumeSentence("sous-composants : \n")
        oneOrMore("subcomponentGroup-#", separator = "\n") {
            consume("-")
            node("size") { componentGroupSize() }
            identifier()  [  "subcomponentGroupName"  ]
            consume("(")
            identifier()  [  "subcomponentGroupType"  ]
            consume(")")
        }
    }

    private fun componentGroupSize() {
        var min: String? = null
        var max: String? = null

        choice {
            option {
                val pMin = integer()
                consume("-")
                val pMax = integer()
                min = pMin
                max = pMax
            }
            option {
                val pMax = integer()
                consume("-")
                min = "0"
                max = pMax
            }
            option {
                val pMin = integer()
                consume("+")
                min = pMin
                max = "-1"
            }
            option {
                val size = integer()
                min = size
                max = size
            }
        }

        min!!  [ "min" ]
        max!!  [ "max" ]
    }

    private fun representation() {
        consumeSentence("représenté par : \n")
        consumeRegex("son|sa|ses")
        words().trim() [ "representationField" ]
    }
}