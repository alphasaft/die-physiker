package physics.values.equalities


internal class UnorderedList<out E>(private val elements: List<E>) : Collection<E> by elements {

    override fun equals(other: Any?): Boolean {
        if (other !is UnorderedList<*>) return false
        val remainingElements = elements.toMutableList()
        for (element in other) {
            if (!remainingElements.remove(element)) return false
        }
        return remainingElements.isEmpty()
    }

    override fun hashCode(): Int {
        return elements.sortedBy { it.hashCode() }.hashCode()
    }

    override fun toString(): String {
        return elements.toString()
    }
}
