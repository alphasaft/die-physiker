package physics.components


class ComponentStructure internal constructor(
    extends: Set<ComponentClass> = setOf(),
    init: Component.() -> Unit = {},
    fieldsTemplates: List<Field.Template<*>> = emptyList(),
    subcomponentsGroupsTemplates: List<ComponentBox.Template> = emptyList(),
) {
    private val directBases: Set<ComponentClass> = extends

    val bases: Set<ComponentClass> = directBases + directBases.map { it.structure.bases }.flatten()

    val init: Component.() -> Unit = { directBases.forEach { it.structure.init(this) } ; init(this) }

    val fieldsTemplates: List<Field.Template<*>> = fieldsTemplates + bases.map { it.structure.fieldsTemplates }.flatten()
    val fieldsNames: List<String> = fieldsTemplates.map { it.name }

    val subcomponentsGroupsTemplates: List<ComponentBox.Template> = subcomponentsGroupsTemplates + bases.map { it.structure.subcomponentsGroupsTemplates }.flatten()
}
