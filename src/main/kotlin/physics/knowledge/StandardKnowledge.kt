package physics.knowledge


import physics.InappropriateKnowledgeException
import physics.QuantityMapper
import physics.ComponentsPickerException
import physics.components.Field
import physics.components.ComponentsPickerWithOutput
import physics.components.Context
import physics.quantities.PValue
import physics.quantities.Quantity
import physics.quantities.castAs


open class StandardKnowledge(
    override val name: String,
    private val specs: ComponentsPickerWithOutput,
    private val mappers: Map<String, QuantityMapper>,
    private val representations: Map<String, String>
) : Knowledge {
    protected val outputVariable = specs.outputVariable
    private val mainMapper = mappers.getValue(outputVariable)
    private val mainRepresentation = representations[outputVariable] ?: "<no_repr>"

    override fun <T : PValue<T>> getFieldValue(field: Field<T>, context: Context): Quantity<T> {
        val appropriateForm = translateToAppropriateFormInOrderToCompute(field, context)
        return appropriateForm.compute(field, context)
    }

    private fun translateToAppropriateFormInOrderToCompute(field: Field<*>, context: Context): StandardKnowledge {
        val fieldName = field.name
        val owner = context.findFieldOwner(field)
        val variable = specs.findVariableCorrespondingTo(field) ?: throw InappropriateKnowledgeException(this, fieldName)

        if (variable == outputVariable) return this
        val refactoredSpecs = specs.isolateVariable(variable)

        return StandardKnowledge(
            name,
            refactoredSpecs,
            mappers,
            representations
        )
    }

    private fun <T : PValue<T>> compute(field: Field<T>, context: Context): Quantity<T> {
        val fieldOwner = context.findFieldOwner(field)
        try {
            val arguments = specs.pickVariablesValues(context, fieldOwner)
            return mainMapper(arguments).castAs(field.type)
        } catch (e: ComponentsPickerException) {
            throw InappropriateKnowledgeException(this, field.name, e.message)
        }
    }

    override fun toString(): String {
        return "$name ($mainRepresentation)"
    }
}
