package physics.computation

import physics.components.Component
import physics.components.Field
import physics.components.PhysicalSystem
import physics.computation.databases.DatabaseConnection
import physics.values.PhysicalString
import physics.values.PhysicalValue
import physics.values.castAs


// TODO : Please, please, refactor this shit
class Database(
    val name: String,
    private val connection: DatabaseConnection,
) {

    // TODO : rename and clean up
    inner class Query(
        override val requirements: List<Requirement>,
        private val columns: Map<String, String>,
        private val output: String,
    ) : PhysicalRelationship() {

        override val inputVariables: List<FormulaVariable> = columns.values.toList().map { v -> FormulaVariable(v, v) }
        override val outputVariable: FormulaVariable = FormulaVariable("O", output)

        override fun <T : PhysicalValue<*>> computeFieldValue(
            field: Field<T>,
            system: PhysicalSystem
        ): Pair<T, Database.Query> {
            val fieldOwner = system.fetchFieldOwner(field)
            val appropriateForm = translateToAppropriateFormInOrderToCompute(field.name, fieldOwner)
            if (appropriateForm !== this) return appropriateForm.computeFieldValue(field, system)

            val arguments = generateArgumentsFor(system, outputOwner = fieldOwner)
            val computed = connection.select(
                field.name,
                where = columns.mapValues { (_, v) -> arguments.getValue(v).toString() }
            )

            return PhysicalString(computed).castAs(field.type) to this
        }

        // TODO : Move this to AbstractPhysicalRelationship
        private fun translateToAppropriateFormInOrderToCompute(field: String, owner: Component): Database.Query {
            if (field == outputVariable.backingField) return this
            val (newOutputRequirement, refactoredRequirements) = refactorRequirementsToFitGivenOutput(field, owner)

            return Query(
                refactoredRequirements,
                columns.filterValues { it != newOutputRequirement.name },
                inputVariables.first { it.represents(field, newOutputRequirement.name) }.let { "${it.owner}.${it.backingField}" },
            )
        }

        override fun toString(): String {
            return "$name avec ${inputVariables.joinToString(", ") { it.backingField }}"
        }
    }
}
