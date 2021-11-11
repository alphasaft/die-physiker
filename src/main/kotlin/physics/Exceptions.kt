package physics

import physics.components.Field
import physics.computation.PhysicalKnowledge
import physics.values.units.PhysicalUnit
import physics.values.PhysicalValue
import kotlin.reflect.KClass


open class PhysicsException(message: String): Exception(message)


open class UnitException(message: String): PhysicsException(message)

class AmbiguousUnitException(signature: PhysicalUnit): UnitException("Unit $signature is ambiguous.")

class IncompatibleUnitsException(unit1: PhysicalUnit, unit2: PhysicalUnit): UnitException("Units $unit1 and $unit2 are incompatible and thus this operation cannot succeed.")

class ConversionNeededException(unit: PhysicalUnit, into: PhysicalUnit): UnitException("A conversion is needed from $unit into $into before proceeding to this operation.")


open class KnowledgeException(message: String) : PhysicsException(message)

class InappropriateKnowledgeException(knowledge: PhysicalKnowledge, toCompute: String, causedBy: String? = null): KnowledgeException("Can't compute $toCompute with $knowledge and the given fields${ if (causedBy != null) " : $causedBy" else "" }")

class UndeclaredComponentException(name: String): KnowledgeException("No component was declared under name '$name'.")

class NoComponentMatchingRequirementsFoundException(ownerName: String, location: String, requiredFields: Collection<String>): KnowledgeException("No component was found in $ownerName.$location that provides known values for fields : ${requiredFields.joinToString(", ")}")

class FieldHasUnknownValueException(field: String): KnowledgeException("Value of field '$field' is unknown.")

class VariableNameCrashError(variable: String) : KnowledgeException("Got two different values for variable $variable.")

class ComponentAliasCrashError(alias: String) : KnowledgeException("Two or more components were registered under the alias $alias.")

internal class EndOfMultiSelection : KnowledgeException("All of the components that meet given requirements were selected")


open class ComponentException(message: String) : PhysicsException(message)

class FieldNotFoundException(field: String, owner: String): ComponentException("$owner(...) doesn't own field $field")

class ComponentGroupNotFoundException(groupName: String, owner: String): ComponentException("$owner(...) doesn't own a subcomponent group named $groupName")

class NoRepresentationProvided(componentName: String) : ComponentException("Component $componentName doesn't possess a custom representation.")

class FieldCastException(field: Field<*>, into: KClass<out PhysicalValue<*>>): ComponentException("Field $field (type ${field.type.simpleName}) cannot be cast into ${into.simpleName}")

class AbstractComponentInitializationError(className: String) : ComponentException("Can't instantiate abstract class $className")



open class DatabaseException(message: String) : Exception(message)

class ColumnNotFoundException(columnName: String) : DatabaseException("Column $columnName doesn't exist.")

class EmptyQueryResult(table: String, column: String, value: PhysicalValue<*>) : DatabaseException("No line was found in table '$table' with '$column' taking the value '$value'.")
