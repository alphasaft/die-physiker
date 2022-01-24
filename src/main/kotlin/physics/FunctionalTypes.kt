package physics

import Args
import physics.quantities.PValue
import physics.quantities.Quantity

typealias QuantityMapper = (Args<Quantity<*>>) -> Quantity<*>
