package physics.rules

class Println(private val message: String) : Action {
    override fun execute(queryResult: QueryResult) {
        println(message)
    }
}