package physics

import physics.specs.FieldSpec


open class RootPhysicalComponentModel(
    name: String,
    fieldSpecs: List<FieldSpec>,
    subComponentsNames: Map<String, ComponentTypeName>,
) : PhysicalComponentModel(name, fieldSpecs, subComponentsNames) {

    inner class Instance internal constructor(
        fields: Map<String, Any?>,
        subComponents: Map<String, List<PhysicalComponent>>
    ) : PhysicalComponent(fields, subComponents) {
        init { computeAll(this) }
    }

    /** Serves as a "constructor" for RootPhysicalComponent (aka RootPhysicalComponentModel.Instance) */
    override operator fun invoke(
        vararg fields: Pair<String, Any?>,
        subComponents: Map<String, List<PhysicalComponent>>,
    ): RootPhysicalComponent {
        return Instance(fields.toMap(), subComponents)
    }
}
