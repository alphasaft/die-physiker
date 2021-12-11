package physics.components

import physics.dynamic.Behavior


class ComponentStructure(
    extends: List<ComponentClass> = emptyList(),
    fieldsTemplates: List<Field.Template<*>> = emptyList(),
    subcomponentsGroupsTemplates: List<ComponentGroup.Template> = emptyList(),
    behaviorsTemplates: List<Behavior.Template> = emptyList(),
) {
    val bases: List<ComponentClass> = extends + extends.map { it.structure.bases }.flatten()
    val fieldsTemplates: List<Field.Template<*>> = fieldsTemplates + bases.map { it.structure.fieldsTemplates }.flatten()
    val fieldsNames: List<String> = fieldsTemplates.map { it.name }
    val subcomponentsGroupsTemplates: List<ComponentGroup.Template> = subcomponentsGroupsTemplates + bases.map { it.structure.subcomponentsGroupsTemplates }.flatten()
    val behaviorsTemplates: List<Behavior.Template> = behaviorsTemplates + bases.map { it.structure.behaviorsTemplates }.flatten()
}