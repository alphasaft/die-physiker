package physics.computation


import physics.InappropriateKnowledgeException
import physics.PhysicalValuesMapper
import physics.RequirementsException
import physics.components.Field
import physics.components.FlexibleRequirementsHandler
import physics.components.PhysicalSystem
import physics.values.PhysicalValue
import physics.values.castAs
import println


open class StandardPhysicalKnowledge(
    name: String,
    private val requirements: FlexibleRequirementsHandler,
    private val mappers: Map<String, PhysicalValuesMapper>,
    private val representations: Map<String, String>
) : BasePhysicalKnowledge(name) {
    protected val outputVariable = requirements.outputVariable
    private val mainMapper = mappers.getValue(outputVariable)
    private val mainRepresentation = representations[outputVariable] ?: "<no_repr>"

    override fun <T : PhysicalValue<*>> renderFor(field: Field<T>, system: PhysicalSystem): String {
        return mainRepresentation
    }

    override fun <T : PhysicalValue<*>> getFieldValueAndObtentionMethod(
        field: Field<T>,
        system: PhysicalSystem
    ): Triple<T, StandardPhysicalKnowledge, String> {
        val appropriateForm = translateToAppropriateFormInOrderToCompute(field, system)
        return Triple(
            appropriateForm.compute(field, system).castAs(field.type),
            appropriateForm,
            appropriateForm.renderFor(field, system)
        )
    }

    private fun translateToAppropriateFormInOrderToCompute(
        field: Field<*>,
        system: PhysicalSystem
    ): StandardPhysicalKnowledge {
        val fieldName = field.name
        val owner = system.findFieldOwner(field)
        val variable = requirements.findVariableCorrespondingTo(fieldName, owner) ?: throw InappropriateKnowledgeException(this, fieldName)

        if (variable == outputVariable) return this
        val refactoredRequirements = requirements.isolateVariable(variable)

        return StandardPhysicalKnowledge(
            name,
            refactoredRequirements,
            mappers,
            representations
        ).finalizeTranslationToAppropriateFormInOrderToCompute(field, system)
    }

    open fun finalizeTranslationToAppropriateFormInOrderToCompute(field: Field<*>, system: PhysicalSystem): StandardPhysicalKnowledge {
        return this
    }

    private fun compute(field: Field<*>, system: PhysicalSystem): PhysicalValue<*> {
        val fieldOwner = system.findFieldOwner(field)
        try {
            val arguments = requirements.getArguments(system, fieldOwner)
            return mainMapper(arguments)
        } catch (e: RequirementsException) {
            throw InappropriateKnowledgeException(this, field.name, e.message)
        }
    }

    override fun toString(): String {
        return name
    }
}
