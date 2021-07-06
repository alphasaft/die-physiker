import loaders.FormulaLoader
import physics.*
import physics.specs.FieldSpec
import dto.removeWhitespaces
import nlp.*
import tokenizing.NLPBaseTokenizer
import java.io.File


fun main() {
    // TODO : Write physical components model loader (shouldn't be hard nor take long)
    // TODO : Fuse together tokenizing module and nlp one, cuz they're so interdependent, and the latter is ultra small
    // TODO : Simply associate words and blah blah blah with concrete components implementation

    val Solution = RootPhysicalComponentModel(
        name = "Solution",
        fieldSpecs = listOf(FieldSpec("mass", Double::class), FieldSpec("volume", Double::class)),
        subComponentsNames = mapOf("solutes" to "Solute", "solvent" to "Liquid"),
    )

    val Solute = PhysicalComponentModel(
        name = "Solute",
        fieldsSpecs = listOf(FieldSpec("mass", Double::class), FieldSpec("density", Double::class)),
    )

    FormulaRegister.addFormulas(FormulaLoader.loadFrom(File(cwd()+"\\resources\\formulas.data")))

    val solute = Solute("mass" to 10.0, "density" to 100.0)
    val solute2 = Solute("mass" to 15.0)
    val solute3 = Solute("density" to 20.0)
    val solution = Solution(subComponents = mapOf("solutes" to listOf(solute, solute2, solute3)))
    println(solution.knownFields, solution.getSubComponents("solutes").map { it.knownFields })

    val corpus = Corpus(
        SimpleWord("I", "MePronoun"),
        WordUnion(listOf(SimpleWord("am"), WordChain(listOf(StrictWord("'"), SimpleWord("m")))),"beVerb")
    )

    println(corpus.match(NLPBaseTokenizer().tokenize("I'm so beautiful").removeWhitespaces()))
}
