package physics


import physics.values.PhysicalValue

typealias Args<T> = Map<String, T>
typealias PhysicalValuesMapper = (args: Args<PhysicalValue<*>>) -> PhysicalValue<*>
