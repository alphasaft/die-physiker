@file:Suppress("LocalVariableName", "SpellCheckingInspection")

import loaders.ComponentClassesLoader
import loaders.KnowledgeLoader
import physics.components.*
import physics.components.ComponentSpec
import physics.components.Location
import physics.components.ComponentsPicker
import physics.functions.Action
import physics.quantities.*
import physics.quantities.doubles.PReal
import physics.quantities.doubles.PRealInterval
import physics.quantities.ints.PInt
import physics.quantities.strings.PString
import physics.values.units.PUnit
import java.io.File
import java.lang.StringBuilder
import kotlin.math.min


fun main() {
    val a = PReal(25.0, significantDigitsCount = 2, unit = PUnit())
    val b = PReal(4.7, significantDigitsCount = 4, unit = PUnit())
    println(a + b)
}


fun main2() {

    // TODO : Avoid variables crashes when merging formulas together

    val knowledgeFunctionsRegister = KnowledgeLoader.getFunctionsRegister()
    val componentClassesFunctionsRegister = ComponentClassesLoader.getFunctionsRegister()
    val scriptFile = File("${cwd()}\\resources\\script.mpsi")

    knowledgeFunctionsRegister.addStandardKnowledgeImplementation("configFromAtomicNumber") { arguments ->
        var remainingElectrons = (arguments["Z"]!!.asPValueOr(PInt(0)).convertTo<PInt>()).value
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
        val config = arguments.getValue("config").asPValue().convertTo<PString>()
        PInt(config.split(")").sumOf { it.split("(").first().ifEmpty { "0" }.toInt() })
    }

    val componentClasses = ComponentClassesLoader(emptyList(), componentClassesFunctionsRegister).loadFrom(File("${cwd()}\\resources\\components.data"))
    val knowledge = KnowledgeLoader(componentClasses, knowledgeFunctionsRegister).loadFrom(File("${cwd()}\\resources\\formulas.data"))

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

    val oxydoreduction = Action(
        "Oxdoréduction",

        ComponentsPicker(
            ComponentSpec.single("S", type = Solution, location = Location.Any, variables = emptyMap()),
            ComponentSpec.single("X", type = Atome, location = Location.At("S.solutés"), variables = mapOf("xSymbol" to "symbole")),
            ComponentSpec.single("Y", type = Atome, location = Location.At("S.solutés"), variables = mapOf("ySymbol" to "symbole"), condition = condition@ {
                component, components ->
                oxydoreductionBetweenTheseExists(
                    component.getQuantity("symbole").asPValueOrNull()?.toPString() ?: return@condition false,
                    components.getValue("X").getQuantity("symbole").asPValueOrNull()?.toPString() ?: return@condition false,
                )
            })
        ),

        actionImpl = ::oxydoreductionImpl
    )

    val solvant = Solvant(componentName = "Sol", fieldValues = mapOf("volume" to PReal("0.70 L")))
    val solute1 = Solute(fieldValues = mapOf("volume" to PReal("0.20 L"), "masse" to PReal("20 g")))
    val solute2 = Solute(fieldValues = mapOf("volume" to PReal("0.30 L"), "masse volumique" to PReal("20 g.L-1")))
    val solute3 = Solute(fieldValues = mapOf("volume" to PReal("0.30 L")))
    val solutes = listOf(solute1, solute2, solute3)
    val solution = Solution(componentName = "S", subcomponentGroupsContents = mapOf("solutés" to solutes, "solvant" to listOf(solvant)))
    val atome = Atome(fieldValues = mapOf("numéro atomique" to PInt(29)))

    val quantity = QuantityIntersection.new(setOf(
        PRealInterval.new(
            isLowerBoundClosed = true,
            PReal(33.3),
            PReal(34.3),
            isUpperBoundClosed = true
        ),
        PRealInterval.new(
            isLowerBoundClosed = false,
            PReal(33.3),
            PReal(37.5),
            isUpperBoundClosed = false
        )
    ))

    println(quantity)
}
