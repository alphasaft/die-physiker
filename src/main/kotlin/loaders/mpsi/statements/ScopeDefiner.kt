package loaders.mpsi.statements

import loaders.mpsi.MpsiType

internal abstract class ScopeDefiner(private val definedVariable: Pair<String, MpsiType>? = null) : Statement
