package util.xml

import kotlin.reflect.KClass

val Node.name get() = nodeName


inline fun <reified E : Element> Node.childrenOfType() = children.filterIsInstance<E>()

fun <E : Element> Node.childrenOfType(type: KClass<E>) = children.filterIsInstance(type.java)

val Node.onlyNodeChildren get() = childrenOfType<Node>()

fun Node.childrenFilteredBy(predicate: (Element) -> Boolean) = children.filter(predicate)

fun Node.onlyNodeChildrenFilteredBy(predicate: (Node) -> Boolean) = onlyNodeChildren.filter(predicate)

fun Node.nodeChildrenNamed(name: String) = onlyNodeChildrenFilteredBy { it.name == name }


fun Node.fetchRecursivelyAllChildren(predicate: (Element) -> Boolean = { true }): List<Element> {
    val result = mutableListOf<Element>()
    for (child in children) {
        if (predicate(child)) result.add(child)
        if (child is Node && child.children.isNotEmpty()) {
            result.addAll(child.fetchRecursivelyAllChildren(predicate))
        }
    }
    return result
}

inline fun <reified E : Element> Node.fetchRecursivelyAllChildrenOfType(): List<E> =
    fetchRecursivelyAllChildrenOfType(E::class)

fun <E : Element> Node.fetchRecursivelyAllChildrenOfType(type: KClass<E>): List<E> =
    @Suppress("UNCHECKED_CAST") (fetchRecursivelyAllChildren { type.isInstance(it) } as List<E>)

fun Node.fetchRecursivelyAllChildrenNodes(predicate: (Node) -> Boolean = { true }) =
    fetchRecursivelyAllChildrenOfType<Node>().filter(predicate)

fun Node.fetchRecursivelyAllChildrenNodesNamed(name: String) =
    fetchRecursivelyAllChildrenNodes { it.name == name }


fun Node.requireAttribute(attributeName: String): String =
    (this[attributeName] ?: throw IllegalArgumentException("Can't find attribute $attributeName in node : \n$this"))

fun Node.requireChild(predicate: (Element) -> Boolean) =
    childrenFilteredBy(predicate)
        .also { require(it.isNotEmpty()) { "Can't find a child satisfying the given predicate in node : \n$this" }}
        .first()

fun Node.requireNodeChild(predicate: (Node) -> Boolean) =
    requireChild { it is Node && predicate(it) } as Node

fun Node.requireNodeChildNamed(nodeName: String) =
    try { requireNodeChild { it.name == nodeName } }
    catch (e: IllegalArgumentException) { throw IllegalArgumentException("Can't find a child named <$nodeName> in node : \n$this") }

