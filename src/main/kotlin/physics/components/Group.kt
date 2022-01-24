package physics.components


class Group internal constructor(
    val name: String,
    private val minimumSize: Int = 0,
    private val maximumSize: Int = -1,
    private val contentType: ComponentClass,
    content: List<Component> = emptyList()
) : AbstractList<Component>() {
    private val _content = content.toMutableList()
    val content: List<Component> get() = _content
    override val size: Int = content.size

    init {
        val illegalContent = content.find { !it.componentClass.inheritsOf(contentType) }
        if (illegalContent != null) throw IllegalArgumentException("Can't pass ${illegalContent.className} where ${contentType.name} is expected.")
    }

    override fun get(index: Int): Component = content[index]

    private fun checkSize() {
        if (size < minimumSize || (maximumSize != -1 && size > maximumSize)) {
            throw IllegalArgumentException("Size of component group '$name' is not within its bounds (from $minimumSize to $maximumSize).")
        }
    }

    override fun toString(): String {
        return if (content.isEmpty()) "$name : NONE" else {
            if (maximumSize == 1) "$name : ${content.single()}"
            else "$name : [\n${joinToString(",\n")}".replace("\n", "\n    ") + "\n]"
        }
    }

    override operator fun contains(element: Component): Boolean =
        content.any { element === it || element in it }

    fun addElement(element: Component) {
        _content.add(element)
        checkSize()
    }

    fun removeElement(element: Component) {
        _content.removeAll { it === element }
        checkSize()
    }

    class Template(
        val name: String,
        private val contentType: ComponentClass,
        private val minimumSize: Int = 0,
        private val maximumSize: Int = -1
    ) {
        fun newGroup(content: List<Component>): Group = Group(
            name,
            minimumSize,
            maximumSize,
            contentType,
            content
        ).apply { checkSize() }
    }
}
