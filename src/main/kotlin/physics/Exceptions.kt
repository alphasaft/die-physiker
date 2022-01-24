package physics

import physics.knowledge.Knowledge


open class PhysicsException(message: String): Exception(message)


class UnitException(message: String): PhysicsException(message)


class InappropriateKnowledgeException(knowledge: Knowledge, toCompute: String, causedBy: String? = null): PhysicsException("Can't compute $toCompute with $knowledge and the given fields${ if (causedBy != null) " : $causedBy" else "" }")


open class ComponentsPickerException(message: String) : PhysicsException(message)


class ComponentException(message: String) : PhysicsException(message)
