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
        string().trim('"')  [ "name" ]
    }

    private fun file() {
        consumeSentence("dans le fichier :")
        string().trim('"')  [ "fileName" ]
    }

    private fun concerns() {
        consumeSentence("concerne :")
        identifier()  [ "componentClass" ]
    }

    private fun links() {
        consumeSentence("lie : \n")
        node("links") {
            oneOrMore("link-#", "\n") {
                consume("-")
                consumeRegex("[a-zA-Z0-9àéèùç ]+").trim()  [ "fieldName" ]
                consume("->")
                identifier()  [ "column" ]
            }
        }
    }

    private fun options() {
        consumeSentence("options : \n")
        node("options") {
            oneOrMore("option-#", "\n") {
                consume("-")
                identifier()  [ "optionName" ]
            }
        }
    }
}