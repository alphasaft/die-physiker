package physics.computation.others

import physics.InappropriateKnowledgeException
import physics.KnowledgeException
import physics.components.Component
import physics.components.Field
import physics.components.PhysicalSystem
import physics.computation.AbstractPhysicalKnowledge
import physics.computation.ComponentRequirement
import physics.computation.Location
import physics.values.PhysicalValue

class ComplexKnowledge(
    name: String,
    requirements: List<ComponentRequirement>,
    output: Pair<String, Location.At>,
    private val mappers: Map<String, (Map<String, PhysicalValue<*>>) -> PhysicalValue<*>>,
) : AbstractPhysicalKnowledge(
    name,
    requirements,
    output,
) {
    constructor(
        name: String,
        vararg requirements: ComponentRequirement,
        output: Pair<String, Location.At>,
        mappers: Map<String, (Map<String, PhysicalValue<*>>) -> PhysicalValue<*>>
    ): this(name, requirements.toList(), output, mappers)

    private val mainMapper = mappers.getValue(outputVariable)

    override fun translateToAppropriateFormInOrderToCompute(
        fieldName: String,
        owner: Component,
        system: PhysicalSystem
    ): AbstractPhysicalKnowledge {
        val variable = findVariableCorrespondingTo(fieldName, owner)
        if (variable == outputVariable) return this

        val newOutputAlias = requirements.single { variable in it.ownedVariables }.alias
        val requirements = refactorRequirementsToIsolateVariable(variable)
        return ComplexKnowledge(
            name,
            requirements,
            variable to Location.At(newOutputAlias, fieldName),
            mappers
        )
    }

    override fun compute(field: Field<*>, system: PhysicalSystem): PhysicalValue<*> {
        val fieldOwner = system.findFieldOwner(field)
        try {
            val arguments = fetchVariablesValuesIn(system, fieldOwner)
            return mainMapper(arguments)
        } catch (e: KnowledgeException) {
            throw InappropriateKnowledgeException(this, field.name, e.message)
        }
    }

    override fun toString(): String {
        return name
    }
}
