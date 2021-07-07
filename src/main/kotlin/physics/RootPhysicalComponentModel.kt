package physics

import physics.specs.ComponentSpec
import physics.specs.FieldSpec
import physics.specs.ProxySpec


open class RootPhysicalComponentModel(
    name: String,
    fieldSpecs: List<FieldSpec>,
    proxiesSpecs: List<ProxySpec> = emptyList(),
    subComponentsSpecs: List<ComponentSpec> = emptyList(),
) : PhysicalComponentModel(name, fieldSpecs, proxiesSpecs, subComponentsSpecs) {

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
