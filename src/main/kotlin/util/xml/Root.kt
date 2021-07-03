package util.xml

import util.generate

private typealias Root = Node

fun Root.parentOf(child: Node): Node? =
    when (child) {
        this -> null
        in children -> this
        else -> this.fetchRecursivelyAllChildrenNodes { child in it.children }.single()
    }

fun Root.parentsOf(child: Node) = generate(first = parentOf(child)) { last -> parentOf(last) }

/**
 * Searches for the given attribute in all of the parents of the [node] that pass the given [filter] (which is equal
 * to { true } if not provided) and the node itself. If [limit] is provided and other than -1, then the algorithm won't
 * search beyond that much parent nodes above, else it will continue to search until it reaches the root.
 *
 * This allow you to declare tags grouping the attributes of the children tags.
 * For instance :
 *
 *      <config for="unit3">
 *          <opt>...</opt>
 *          <opt>...</opt>
 *          <opt>...</opt>
 *      </config>
 *
 * Instead of :
 *
 *      <opt for="unit3">...</opt>
 *      <opt for="unit3">...</opt>
 *      <opt for="unit3">...</opt>
 *
 * Calling searchAttributeInParentNodes(optTagNumber2, "for") on the first ast will yield "unit3" too.
 */
fun Root.searchAttributeInParentNodes(
    child: Node,
    attributeName: String,
    filter: (Node) -> Boolean = { true },
    limit: Int = -1,
): String? {
    val parents = listOf(child) + parentsOf(child).filter(filter).let { if (limit == -1) it else it.take(limit) }
    for (parent in parents) {
        parent.get<String>(attributeName)?.let { return it }
    }
    return null
}
