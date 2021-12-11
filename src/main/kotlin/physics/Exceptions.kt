package physics

import physics.components.Component
import physics.components.ComponentClass
import physics.components.Field
import physics.computation.BasePhysicalKnowledge
import physics.values.units.PhysicalUnit
import physics.values.PhysicalValue
import kotlin.reflect.KClass


open class PhysicsException(message: String): Exception(message)


open class UnitException(message: String): PhysicsException(message)

class UnknownUnitException(unit: PhysicalUnit) : UnitException("Unit '$unit' wasn't declared in this scope.")

class AmbiguousUnitException(unit: PhysicalUnit): UnitException("Unit $unit is ambiguous.")

class IncompatibleUnitsException(unit1: PhysicalUnit, unit2: PhysicalUnit): UnitException("Units $unit1 and $unit2 are incompatible and thus this operation cannot succeed.")

class ConversionNeededException(unit: PhysicalUnit, into: PhysicalUnit): UnitException("A conversion is needed from $unit into $into before proceeding to this operation.")


class InappropriateKnowledgeException(knowledge: BasePhysicalKnowledge, toCompute: String, causedBy: String? = null): PhysicsException("Can't compute $toCompute with $knowledge and the given fields${ if (causedBy != null) " : $causedBy" else "" }")


open class RequirementsException(message: String) : PhysicsException(message)

class UndeclaredComponentException(name: String): RequirementsException("No component was declared under name '$name'.")

class NoComponentMatchingRequirementsFoundException(ownerName: String, location: String, requiredFields: Collection<String>): RequirementsException("No component was found in $ownerName.$location that provides known values for fields : ${requiredFields.joinToString(", ")}")

class FieldHasUnknownValueException(field: String): RequirementsException("Value of field '$field' is unknown.")

class VariableNameCrashError(variable: String) : RequirementsException("Got two different values for variable $variable.")

class ComponentAliasCrashError(alias: String) : RequirementsException("Two or more components were registered under the alias $alias.")

internal class EndOfMultiSelection : RequirementsException("All of the components that meet given requirements were selected")


open class ComponentException(message: String) : PhysicsException(message)

class ComponentInstantiationError(className: String, causedBy: ComponentException): ComponentException("When instantiating $className : ${causedBy::class.simpleName} : ${causedBy.message}")

class FieldNotFoundException(field: String, owner: String): ComponentException("$owner(...) doesn't own the field '$field'")

class ComponentGroupNotFoundException(groupName: String, owner: String): ComponentException("$owner(...) doesn't own a subcomponent group named '$groupName'")

class BehaviorNotFoundException(behaviorName: String, owner: String): ComponentException("$owner(...) doesn't own a behavior named '$behaviorName'")

class FieldCastException(field: Field<*>, into: KClass<out PhysicalValue<*>>): ComponentException("Field $field (type ${field.type.simpleName}) cannot be cast into ${into.simpleName}")

class AbstractComponentInstantiationError(className: String) : ComponentException("Can't instantiate abstract class $className")

class MissingBehaviorImplException(message: String) : ComponentException(message)


open class DatabaseException(message: String) : Exception(message)

class ColumnNotFoundException(columnName: String) : DatabaseException("Column $columnName doesn't exist.")

class EmptyQueryResult(table: String, column: String, value: PhysicalValue<*>) : DatabaseException("No line was found in table '$table' with '$column' taking the value '$value'.")
