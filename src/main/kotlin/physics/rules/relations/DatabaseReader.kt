package physics.rules.relations

import physics.quantities.PString
import physics.quantities.Quantity

interface DatabaseReader {
    fun select(fieldName: String, where: Map<String, String>): Quantity<PString>
}
