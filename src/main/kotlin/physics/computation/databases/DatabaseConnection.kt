package physics.computation.databases


interface DatabaseConnection {
    fun select(what: String, where: Map<String, String>): String
}
