package util.xml


fun Node.getText(separator: String = "\n") =
    childrenOfType<TextElement>().filterNot(TextElement::isEmpty).joinToString(separator) { it.text }

val Node.text get() = getText()

fun TextElement.isEmpty() = text.isBlank()
