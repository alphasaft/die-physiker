package physics

import physics.formulas.Formula
import physics.components.Field
import physics.formulas.PhysicalRelationship
import physics.units.PhysicalUnit
import physics.values.PhysicalValue
import kotlin.reflect.KClass


open class PhysicsException(message: String): Exception(message)

open class UnitException(message: String): PhysicsException(message)

class AmbiguousUnitException(signature: PhysicalUnit): UnitException("Unit $signature is ambiguous.")

class IncompatibleUnitsException(unit1: PhysicalUnit, unit2: PhysicalUnit): UnitException("Units $unit1 and $unit2 are incompatible and thus this operation cannot succeed.")

class ConversionNeededException(unit: PhysicalUnit, into: PhysicalUnit): UnitException("A conversion is needed from $unit into $into before proceeding to this operation.")

open class FormulaException(message: String): PhysicsException(message)

class InappropriateFormula(formula: PhysicalRelationship, toCompute: String, causedBy: String? = null): FormulaException("Can't compute $toCompute with $formula and the given fields${ if (causedBy != null) " : $causedBy" else "" }")

class FieldNotFoundException(field: String, owner: String): FormulaException("$owner(...) doesn't own field $field")

class ComponentGroupNotFoundException(groupName: String, owner: String): FormulaException("$owner(...) doesn't own a subcomponent group named $groupName")

class FieldCastException(field: Field<*>, into: KClass<out PhysicalValue<*>>): FormulaException("Field $field (type ${field.type.simpleName}) cannot be cast into ${into.simpleName}")

class UndeclaredComponentException(name: String): FormulaException("No component was declared under name '$name'.")

class NoComponentMatchingRequirementsFoundException(ownerName: String, location: String, requiredFields: List<String>): FormulaException("No component was found in $ownerName.$location that provides known values for fields : ${requiredFields.joinToString(", ")}")

class FieldHasUnknownValueException(field: String): FormulaException("Value of field '$field' is unknown.")

