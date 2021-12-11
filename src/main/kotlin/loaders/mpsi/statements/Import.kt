package loaders.mpsi.statements

data class Import(val className: String) {
    override fun toString(): String {
        return "import $className"
    }
}