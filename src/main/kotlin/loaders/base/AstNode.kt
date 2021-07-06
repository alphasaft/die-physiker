package loaders.base

open class AstNode {
    protected val branchesStorage = mutableSetOf<List<String>>()
    val branches: Set<List<String>> get() = branchesStorage

    private val childrenStorage = mutableMapOf<String, AstNode>()
    val children: Map<String, AstNode> get() = childrenStorage
    val size get() = children.size

    private var contentSet = false
    var content: String? = null
        set(value) {
            requireUnlocked()
            if (contentSet) throw IllegalArgumentException("Can't set twice the content of an AstNode")
            field = value
            contentSet = true
        }

    private var locked = false

    fun lock() {
        locked = true
        for (child in children.values) child.lock()
    }

    protected fun requireUnlocked() {
        if (locked) throw IllegalArgumentException("Can't mutate immutable node $this")
    }

    private fun addAllBranchesFor(branchPath: List<String>, node: AstNode) {
        branchesStorage.add(branchPath)
        branchesStorage.addAll(node.branches.map { branchPath + it })
    }

    fun getNode(nodePos: String) = getNode(listOf(nodePos))
    fun getNode(nodePath: List<String>): AstNode {
        return when (nodePath.size) {
            0 -> this
            1 -> children.getValue(nodePath.single())
            else -> children.getValue(nodePath.first()).getNode(nodePath.subList(1, nodePath.size))
        }

    }

    fun allNodes(genericNodePos: String) = allNodes(listOf(genericNodePos))
    fun allNodes(genericNodePath: List<String>): List<AstNode> {
        require('#' in genericNodePath.last()) { "Last item of the node path must contain '#'" }

        val prePath = genericNodePath.subList(0, genericNodePath.size-1)
        val lastItem = genericNodePath.last()
        val result = mutableListOf<AstNode>()
        var i = 1

        while (hasChildNode(prePath + lastItem.replace("#", i.toString()))) {
            result.add(getNode(prePath + lastItem.replace("#", i.toString())))
            i++
        }

        return result
    }

    fun hasChildNode(nodePos: String) = hasChildNode(listOf(nodePos))
    fun hasChildNode(nodePath: List<String>): Boolean {
        return try {
            getNode(nodePath)
            true
        } catch (e: NoSuchElementException) {
            false
        }
    }

    fun setNode(nodePos: String, node: AstNode) = setNode(listOf(nodePos), node)
    fun setNode(nodePath: List<String>, node: AstNode) {
        requireUnlocked()
        if (nodePath.size == 1) childrenStorage[nodePath.single()] = node
        else {
            if (nodePath.first() !in children) childrenStorage[nodePath.first()] = AstNode()
            this.getNode(nodePath.first()).setNode(nodePath.subList(1, nodePath.size), node)
        }
        addAllBranchesFor(nodePath, node)
    }

    fun cutBranch(branchPath: List<String>) {
        requireUnlocked()
        if (branchPath.size == 1) childrenStorage.remove(branchPath.first())
        else children.getValue(branchPath.first()).cutBranch(branchPath.subList(1, branchPath.size))
    }

    operator fun get(childName: String): String {
        return getOrNull(childName) ?: throw IllegalArgumentException("Field $childName is null ; use getOrNull instead.")
    }

    fun getOrNull(fieldName: String): String? {
        return try {
            getNode(fieldName.split(".")).content
        } catch (e: NoSuchElementException) {
            null
        }
    }

    override fun toString(): String {
        return toString(indent = 0)
    }

    open fun toString(indent: Int): String {
        val baseIndent = "\t".repeat(indent)
        return "AstNode(\n" +
               "    $baseIndent${children.toList().joinToString(",\n\t$baseIndent") { (k, v) -> "$k=${v.toString(indent+1)}" }}\n" +
               "$baseIndent)"
    }
}