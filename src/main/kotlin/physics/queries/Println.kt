package physics.queries

class Println(private val message: String) : Action {
    override fun execute(queryResult: QueryResult) {
        println(message)
    }
}