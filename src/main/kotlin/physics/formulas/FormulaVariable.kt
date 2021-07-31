package physics.formulas

import physics.components.Component
import physics.components.Field
import physics.values.PhysicalValue

class FormulaVariable(
    val name: String,
    backingField: String,
) {
    internal val owner = backingField.split(".").first()
    internal val backingField = backingField.split(".").last()

    fun represents(field: String, owner: String) = field == this.backingField && owner == this.owner

    fun findCorrespondingFieldIn(selectedComponents: Map<String, Component>): Field<*> {
        return selectedComponents.getValue(owner).getField<PhysicalValue<*>>(backingField)
    }

    override fun equals(other: Any?): Boolean {
        return other is FormulaVariable && owner == other.owner && name == other.name && backingField == other.backingField
    }

    fun renameOwner(newOwner: String): FormulaVariable {
        return FormulaVariable(name, "$newOwner.$backingField")
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + backingField.hashCode()
        return result
    }
}