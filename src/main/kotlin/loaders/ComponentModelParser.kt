package loaders

import loaders.base.Parser

object ComponentModelParser : Parser() {
    override val whitespaces: String = " \t\r\n"

    override fun axiom() {
        oneOrMore("component-#") {
            header()
            all {
                optionalBlock { fields() }
                optionalBlock { subComponents() }
                optionalBlock { proxies() }
            }
        }
    }

    private fun header() {
        choice {
            option {
                "yes"  [ "isRoot" ]
                consume("Root")
                consume("component")
            }

            option {
                "no"  [ "isRoot" ]
                consume("Component")
            }
        }
        consume(identifier)  [ "name" ]
    }

    private fun fields() {
        consume("owns")
        consume(":")
        oneOrMore("field-#") {
            field()
        }
    }

    private fun field() {
        consumeRegex("an?")
        consume(identifier)  [ "name" ]
        consume("(")
        consume(identifier)  [ "type" ]
        consume(")")
    }

    private fun subComponents() {
        consume("subcomponents")
        consume(":")
        oneOrMore("subcomponent-#") {
            subComponent()
        }
    }

    private fun subComponent() {
        subComponentQuantity()
        consume(identifier)  [ "name" ]
        consume("(")
        consume(identifier)  [ "type" ]
        consume(")")
    }

    private fun subComponentQuantity() {
        consume("{")
        choice {
            option {
                consume(integer)  [ "atLeast" ]
                consume("-")
                consume(integer)  [ "atMost" ]
            }

            option {
                consume(integer)  [ "atLeast" ]
                consume("+") ; "-1"  [ "atMost" ]
            }

            option {
                consume(integer)  [ "atMost" ]
                consume("-") ; "0"  [ "atLeast" ]
            }

            option {
                consume(integer)  [ "atLeast" ]  [ "atMost" ]
            }

            option {
                consume("*")
                "0"  [ "atLeast" ]
                "-1"  [ "atMost" ]
            }

            option {
                consume("+")
                "1"  [ "atLeast" ]
                "-1" [ "atMost" ]
            }
        }
        consume("}")
    }

    private fun proxies() {
        consume("proxies")
        consume(":")
        oneOrMore("proxy-#") {
            proxy()
        }
    }

    fun proxy() {
        consume(identifier)  [ "name" ]
        consume("->")
        consume(identifier)  [ "target" ]
    }
}