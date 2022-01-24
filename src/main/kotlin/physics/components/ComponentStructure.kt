package physics.components


class ComponentStructure(
    extends: List<ComponentClass> = emptyList(),
    init: Component.() -> Unit = {},
    fieldsTemplates: List<Field.Template<*>> = emptyList(),
    subcomponentsGroupsTemplates: List<Group.Template> = emptyList(),
) {
    private val directBases: List<ComponentClass> = extends
    val bases: List<ComponentClass> = directBases + directBases.map { it.structure.bases }.flatten()

    val init: Component.() -> Unit = { directBases.forEach { it.structure.init(this) } ; init(this) }

    val fieldsTemplates: List<Field.Template<*>> = fieldsTemplates + bases.map { it.structure.fieldsTemplates }.flatten()
    val fieldsNames: List<String> = fieldsTemplates.map { it.name }

    val subcomponentsGroupsTemplates: List<Group.Template> = subcomponentsGroupsTemplates + bases.map { it.structure.subcomponentsGroupsTemplates }.flatten()
}
