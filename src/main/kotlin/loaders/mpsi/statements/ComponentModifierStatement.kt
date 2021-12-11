package loaders.mpsi.statements

internal class ComponentModifierStatement(
    private val modifiedComponent: String,
    private val fields: List<FieldSetter>,
    private val componentGroupsModifiers: List<ComponentGroupModifier>
) : Statement {
    override fun toString(): String {
        return "modify $modifiedComponent { ... }"
    }
}
