@file:Suppress("LocalVariableName", "SpellCheckingInspection")

import loaders.ComponentClassesLoader
import loaders.KnowledgeLoader
import loaders.UnitScopeLoader
import loaders.mpsi.ScriptLoader
import loaders.mpsi.ScriptParser
import physics.components.*
import physics.dynamic.Action
import physics.values.*
import java.io.File
import java.lang.StringBuilder
import kotlin.math.min


fun main() {

    // TODO : Avoid variables crashes when merging formulas together


    val scope = UnitScopeLoader.loadFrom(File("${cwd()}\\resources\\units.data"))
    val valuesFactory = PhysicalValuesFactory(scope)
    val knowledgeFunctionsRegister = KnowledgeLoader.getFunctionsRegister()
    val componentClassesFunctionsRegister = ComponentClassesLoader.getFunctionsRegister()
    val scriptFile = File("${cwd()}\\resources\\script.mpsi")

    knowledgeFunctionsRegister.addStandardKnowledgeImplementation("configFromAtomicNumber") { arguments ->
        var remainingElectrons = (arguments["Z"]!!.toPhysicalInt()).value
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

        valuesFactory.string(config.toString())
    }

    knowledgeFunctionsRegister.addStandardKnowledgeImplementation("atomicNumberFromConfig") { arguments ->
        val config = arguments.getValue("config").toPhysicalString()
        valuesFactory.int(config.split(")").sumOf { it.split("(").first().ifEmpty { "0" }.toInt() })
    }

    val componentClasses = ComponentClassesLoader(valuesFactory, emptyList(), componentClassesFunctionsRegister).loadFrom(File("${cwd()}\\resources\\components.data"))
    val knowledge = KnowledgeLoader(componentClasses, knowledgeFunctionsRegister).loadFrom(File("${cwd()}\\resources\\formulas.data"))

    val constants = ComponentClass("Constants",
        structure = ComponentStructure(
            fieldsTemplates = listOf(
                Field.Template("G", valuesFactory.doubleFactoryWithUnit("N.kg-2.m2")),
                Field.Template("g", valuesFactory.doubleFactoryWithUnit("N.kg-1")),
            )
        )
    ).invoke(
        fieldValuesAsStrings = mapOf(
            "G" to "6.67*10^-11",
            "g" to "9.81",
        )
    )

    val Solvant = componentClasses.getValue("Solvant")
    val Solute = componentClasses.getValue("Soluté")
    val Solution = componentClasses.getValue("Solution")
    val Atome = componentClasses.getValue("Atome")

    val myReactions = mapOf(
        "Cu,H" to "Fe,Ni"
    )

    fun oxydoreductionBetweenTheseExists(c1: String, c2: String): Boolean = "$c1,$c2" in myReactions

    fun oxydoreductionImpl(components: Map<String, Component>, arguments: Map<String, PhysicalValue<*>>) {
        val (xName, yName) = arguments.getValue("xName") to arguments.getValue("yName")
        val (newX, newY) = myReactions.getValue("$xName,$yName").split(",").let { (p1, p2) -> Atome(fieldValuesAsStrings = mapOf("name" to p1)) to Atome(fieldValuesAsStrings = mapOf("name" to p2)) }

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

        RequirementsHandler(
            ComponentRequirement.single("S", type = Solution, location = Location.Any, variables = emptyMap()),
            ComponentRequirement.single("X", type = Atome, location = Location.At("S.solutés"), variables = mapOf("xSymbol" to "symbole")),
            ComponentRequirement.single("Y", type = Atome, location = Location.At("S.solutés"), variables = mapOf("ySymbol" to "symbole"), condition = {
                component, components ->
                oxydoreductionBetweenTheseExists(component["symbole"], components.getValue("X")["symbole"])
            })
        ),

        actionImpl = ::oxydoreductionImpl
    )

    val solvant = Solvant(fieldValuesAsStrings = mapOf("volume" to "Ve = 0.70 L"))
    val solute1 = Solute(fieldValuesAsStrings = mapOf("volume" to "0.20 L", "masse" to "20 g"))
    val solute2 = Solute(fieldValuesAsStrings = mapOf("volume" to "0.30 L", "masse volumique" to "20 g.L-1"))
    val solute3 = Solute(fieldValuesAsStrings = mapOf("volume" to "0.30 L"))
    val solutes = listOf(solute1, solute2, solute3)
    val solution = Solution(subcomponentGroupsContents = mapOf("solutés" to solutes, "solvant" to listOf(solvant)))
    val atome = Atome(fieldValuesAsStrings = mapOf("numéro atomique" to "29"))

    solution.fillFieldsWithTheirValuesUsing(knowledge)
    atome.fillFieldsWithTheirValuesUsing(knowledge)

    println(ScriptParser.parse(scriptFile))
    println(ScriptLoader(valuesFactory, componentClasses).loadFrom(scriptFile))
}
