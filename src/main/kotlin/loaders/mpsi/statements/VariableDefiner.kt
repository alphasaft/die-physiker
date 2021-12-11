package loaders.mpsi.statements

import loaders.mpsi.MpsiType


internal abstract class VariableDefiner(val variables: Map<String, MpsiType>) : Statement {
    constructor(variable: Pair<String, MpsiType>): this(mapOf(variable))
}
