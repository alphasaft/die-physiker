import loaders.ComponentModelsLoader
import loaders.FormulaLoader
import physics.*
import nlp.*
import java.io.File


fun main() {
    // TODO : Elaborate the concept of "proxies" for components
    // TODO : Simply associate words and blah blah blah with concrete components implementation

    val formulas = FormulaLoader.loadFrom(File(cwd()+"\\resources\\formulas.data")))
    val models = ComponentModelsLoader.loadFrom(File(cwd()+"\\resources\\components.data"))
    FormulaRegister.addFormulas(formulas)

    val Solute = models.getValue("Solute")
    val Solution = models.getValue("Solution")
    val Liquid = models.getValue("Liquid")

    val solute = Solute("density" to 10.0)
    val solute2 = Solute("mass" to 15.0, "density" to 20.0)
    val solute3 = Solute("density" to 20.0)
    val solvent = Liquid("volume" to 2.0)
    val solution = Solution(subComponents = mapOf("solutes" to listOf(solute, solute2, solute3), "solvent" to listOf(solvent)))

    println(
        solution.knownFields,
        solution.getSubComponents("solutes").map { it.knownFields },
        solution.getSubComponent("solvent")!!.knownFields
    )
}
