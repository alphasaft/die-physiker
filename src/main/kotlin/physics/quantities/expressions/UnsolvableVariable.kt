package physics.quantities.expressions

class UnsolvableVariable(variable: String) : Exception("Variable $variable can't be isolated.")
