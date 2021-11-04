package physics.computation.others

import physics.components.Component
import physics.components.Field
import physics.components.PhysicalSystem
import physics.computation.AbstractPhysicalKnowledge
import physics.computation.ComponentRequirement
import physics.computation.PhysicalKnowledge
import physics.values.PhysicalValue
import physics.values.castAs

class ComplexKnowledge(
    val name: String,
    requirements: List<ComponentRequirement>,
    output: Pair<String, String>,
    private val mappers: Map<String, (Map<String, PhysicalValue<*>>) -> PhysicalValue<*>>,
) : AbstractPhysicalKnowledge(
    requirements,
    output,
) {
    constructor(
        name: String,
        vararg requirements: ComponentRequirement,
        output: Pair<String, String>,
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

        val newOutputRequirement = requirements.single { variable in it.ownedVariables }
        val requirements = refactorRequirementsToIsolateVariable(variable)
        return ComplexKnowledge(
            name,
            requirements,
            variable to "${newOutputRequirement.alias}.$fieldName",
            mappers
        )
    }

    override fun compute(field: Field<*>, system: PhysicalSystem): PhysicalValue<*> {
        val fieldOwner = system.findFieldOwner(field)
        val arguments = fetchVariablesValuesIn(system, fieldOwner)
        return mainMapper(arguments)
    }

    override fun toString(): String {
        return name
    }
}
