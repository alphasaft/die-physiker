package physics

object FormulaRegister {
    val formulas = mutableListOf<Formula>()

    fun addFormula(formula: Formula) {
        this.formulas.add(formula)
    }

    fun addFormulas(formulas: List<Formula>) {
        this.formulas.addAll(formulas)
    }
}