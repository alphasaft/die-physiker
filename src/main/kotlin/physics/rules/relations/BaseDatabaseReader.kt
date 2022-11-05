package physics.rules.relations

import includedIn
import physics.quantities.*


open class BaseDatabaseReader(val cells: List<Map<String, String>>) : DatabaseReader {
    override fun select(fieldName: String, where: Map<String, String>): Quantity<PString> {
        var result: Quantity<PString> = ImpossibleQuantity<PString>()
        for (line in cells) if (where includedIn line) result = result union PString(line.getValue(fieldName))
        return result
    }
}