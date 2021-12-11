package loaders.mpsi.statements

import physics.components.ComponentClass

internal class ComponentBuilderExpression(
    private val componentClass: ComponentClass,
    private val fieldsBuilders: List<FieldSetter>,
    private val subcomponentsGroupsModifiers: List<ComponentGroupModifier>
) : Expression() {
    override fun toString(): String {
        return "create ${componentClass.name} { ... }"
    }
}
