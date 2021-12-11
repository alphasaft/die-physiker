package loaders.base

abstract class Parser : ParserLogic() {

    protected operator fun String.invoke() = consumeRegex(this)
    protected val string = "\".*?\""
    protected val letter = "[a-zA-Zéèçàùµ]"
    protected val identifier = "[${letter}_][${letter}\\d_]*"
    protected val words = "[_$letter][ _0-9$letter]*"
    protected val integer = "-?\\d+"
    protected val double = "-?\\d+\\.\\d+"
    protected open val nl = "\n+"
    protected open val nlOpt = "\n*"


    protected fun collectionPattern(separator: String, argName: String, item: () -> Unit) {
        zeroOrMore(argName, separator) {
            item()
        }
    }
    
    protected fun mapPattern(
        separator: String,
        keyToValueSymbol: String,
        argName: String,
        key: Pair<String, () -> Any>,
        value: Pair<String, () -> Any>
    ) =
        collectionPattern(separator, argName) {
            node(key.first) { key.second() }
            consume(keyToValueSymbol)
            node(value.first) { value.second() }
        }
}