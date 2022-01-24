package physics.quantities.ints

import physics.quantities.PInterval


class PIntInterval(
    lowerBound: PInt,
    upperBound: PInt,
) : PInterval<PInt>(
    type = PInt::class,
    isLowerBoundClosed = true,
    lowerBound,
    upperBound,
    isUpperBoundClosed = true
) {

    // It is safe to assume isLowerBoundClosed and isUpperBoundClosed are always passed as true here
    override fun new(
        isLowerBoundClosed: Boolean,
        lowerBound: PInt,
        upperBound: PInt,
        isUpperBoundClosed: Boolean
    ) = PIntInterval(
        lowerBound,
        upperBound
    )
}
