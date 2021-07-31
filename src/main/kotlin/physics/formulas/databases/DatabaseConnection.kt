package physics.formulas.databases


interface DatabaseConnection {
    fun select(what: String, where: DatabaseCondition): String
}
