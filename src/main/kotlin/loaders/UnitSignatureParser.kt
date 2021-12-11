package loaders

import loaders.base.Parser

object UnitSignatureParser : Parser() {
    override fun axiom() {
        oneOrMore("signaturePart-#", separator = "\\.") {
            consumeRegex("[a-zA-Z]+")  [ "unit" ]
            var exponent = "1"
            optional {
                exponent = integer()
            }
            exponent  [ "exponent" ]
        }
    }
}
