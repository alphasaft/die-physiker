@file:Suppress("LocalVariableName")

import loaders.ComponentClassesLoader
import loaders.KnowledgeLoader
import physics.components.*
import physics.quantities.*
import physics.quantities.PInt
import physics.quantities.PString
import physics.quantities.expressions.div
import physics.quantities.expressions.equal
import physics.quantities.expressions.v
import physics.queries.*
import java.io.File
import java.lang.StringBuilder
import kotlin.math.min


fun main() {

    // TODO : Avoid variables crashes when merging formulas together

    val knowledgeFunctionsRegister = KnowledgeLoader.getFunctionsRegister()
    val componentClassesFunctionsRegister = ComponentClassesLoader.getFunctionsRegister()
    val scriptFile = File("${cwd()}\\resources\\script.mpsi")

    knowledgeFunctionsRegister.addStandardKnowledgeImplementation("configFromAtomicNumber") { arguments ->
        var remainingElectrons = (arguments["Z"]!!.asPValueOr(PInt(0)).useAs<PInt>()).value
        val subshellNames = mapOf(0 to "s", 1 to "p", 2 to "d", 3 to "f")
        val config = StringBuilder()

        var currentEnergyLevel = 1
        var shell = 1
        var subshell = 0

        while (remainingElectrons > 0) {
            val electronsOnTheSubshell = min(4*subshell + 2, remainingElectrons)
            remainingElectrons -= electronsOnTheSubshell

            config.append("($shell${subshellNames[subshell]})$electronsOnTheSubshell")
            shell++
            subshell--

            if (subshell < 0) {
                currentEnergyLevel++
                shell = currentEnergyLevel / 2 + 1
                subshell = currentEnergyLevel - shell
            }
        }

        PString(config.toString())
    }

    knowledgeFunctionsRegister.addStandardKnowledgeImplementation("atomicNumberFromConfig") { arguments ->
        val config = arguments.getValue("config").asPValue().useAs<PString>()
        PInt(config.split(")").sumOf { it.split("(").first().ifEmpty { "0" }.toInt() })
    }

    val componentClasses = ComponentClassesLoader(emptyList(), componentClassesFunctionsRegister).loadFrom(File("${cwd()}\\resources\\components.data"))

    val Solvant = componentClasses.getValue("Solvant")
    val Solute = componentClasses.getValue("Soluté")
    val Solution = componentClasses.getValue("Solution")
    val Atome = componentClasses.getValue("Atome")

    val myReactions = mapOf(
        "Cu,H" to "Fe,Ni"
    )

    fun oxydoreductionBetweenTheseExists(c1: PString, c2: PString): Boolean = "$c1,$c2" in myReactions

    fun oxydoreductionImpl(components: Map<String, Component>, arguments: Map<String, Quantity<*>>) {
        val (xName, yName) = arguments.getValue("xName") to arguments.getValue("yName")
        val (newX, newY) = myReactions.getValue("$xName,$yName").split(",").let { (p1, p2) -> Atome(fieldValues = mapOf("name" to PString(p1))) to Atome(fieldValues = mapOf("name" to PString(p2))) }

        (components.getValue("S")) {
            group("solutés") {
                -components.getValue("X")
                -components.getValue("Y")
                +newX
                +newY
            }
        }
    }

    val solvant = Solvant()
    val solute1 = Solute(fieldValues = mapOf("masse" to PReal("20 g")))
    val solute2 = Solute(fieldValues = mapOf("masse volumique" to PReal("20 g.L-1")))
    val solute3 = Solute()
    val solutes = listOf(solute1, solute2, solute3)
    val solution = Solution(componentName = "S", fieldValues = mapOf("volume" to PReal("3.0 L")), subcomponentGroupsContents = mapOf("solutés" to solutes, "solvant" to listOf(solvant)))

    val query1 = Query(
        SelectBaseComponent("S", Solution),
        SelectComponent("X", sourceIdentifier = "S", boxName = "solutés"),
        ExtractField("V", sourceIdentifier = "S", fieldName = "volume"),
        ExtractField("m", sourceIdentifier = "X", fieldName = "masse"),
        ExtractField("p", sourceIdentifier = "X", fieldName = "masse volumique")
    )
    val formula1 = Formula(v("p") equal v("m")/v("V"))
    val rule1 = ForEachQueryResult(query1) perform ApplyRelation(formula1)
    rule1.applyOn(solution)

    val query2 = Query(
        SelectBaseComponent("S", Solution),
        SelectComponent("X", sourceIdentifier = "S", boxName = "solvant"),
        ExtractField("Vs", sourceIdentifier = "S", fieldName = "volume"),
        ExtractField("Vx", sourceIdentifier = "X", fieldName = "volume"),
    )
    val formula2 = Formula(v("Vs") equal v("Vx"))
    val rule2 = ForEachQueryResult(query2) perform ApplyRelation(formula2)
    rule2.applyOn(solution)

    println(solution.fullRepresentation())
}
