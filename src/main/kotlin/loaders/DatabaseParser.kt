package loaders

import loaders.base.Parser

object DatabaseParser : Parser() {
    override fun axiom() {
        header()
        consume("\n")
        all(separator = "\n") {
            block {
                file()
            }
            block {
                concerns()
            }
            block {
                links()
            }
            optionalBlock {
                options()
            }
        }
    }

    private fun header() {
        consumeSentence("La base de données")
        consumeRegex(string).trim('"')  [ "name" ]
    }

    private fun file() {
        consumeSentence("dans le fichier :")
        consumeRegex(string).trim('"')  [ "fileName" ]
    }

    private fun concerns() {
        consumeSentence("concerne :")
        consumeRegex(identifier)  [ "componentClass" ]
    }

    private fun links() {
        consumeSentence("lie : \n")
        group("links") {
            oneOrMore("link-#", "\n") {
                consume("-")
                consumeRegex("[a-zA-Z0-9àéèùç ]+").trim()  [ "fieldName" ]
                consume("->")
                consumeRegex(identifier)  [ "column" ]
            }
        }
    }

    private fun options() {
        consumeSentence("options : \n")
        group("options") {
            oneOrMore("option-#", "\n") {
                consume("-")
                consumeRegex(identifier)  [ "optionName" ]
            }
        }
    }
}