package util.xml


fun <T> Node.transformInto(transform: Node.() -> T): T = this.transform()

fun <T> Iterable<Node>.mapNodes(transform: Node.() -> T): List<T> = map { it.transformInto(transform) }
