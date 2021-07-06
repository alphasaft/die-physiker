package loaders.base

class AstLeaf(content: String) : AstNode() {
    init {
        this.content = content
        lock()
    }

    override fun toString(): String {
        return "AstLeaf(${content!!})"
    }

    override fun toString(indent: Int): String {
        return toString()
    }
}
