package physics.components


class ComponentStructure internal constructor(
    extends: Set<ComponentClass> = setOf(),
    init: Component.() -> Unit = {},
    fieldsTemplates: Map<String, Field.Template<*>> = emptyMap(),
    boxesTemplates: Map<String, ComponentBox.Template> = emptyMap(),
    stateEquationsTemplates: Map<String, StateEquation.Template> = emptyMap(),
) {
    private val directBases: Set<ComponentClass> = extends

    val bases: Set<ComponentClass> = directBases + directBases.map { it.structure.bases }.flatten()
    val init: Component.() -> Unit = { directBases.forEach { it.structure.init(this) } ; init(this) }
    val fieldsNames: Set<String> = fieldsTemplates.keys
    val fieldsTemplates: Map<String, Field.Template<*>> = directBases.flatMap { it.structure.fieldsTemplates.toList() }.toMap() + fieldsTemplates
    val boxesTemplates: Map<String, ComponentBox.Template> = directBases.flatMap { it.structure.boxesTemplates.toList() }.toMap() + boxesTemplates
    val equationsTemplates: Map<String, StateEquation.Template> = directBases.flatMap { it.structure.equationsTemplates.toList() }.toMap() + stateEquationsTemplates
}
