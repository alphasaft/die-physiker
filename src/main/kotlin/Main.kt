import loaders.KnowledgeLoader
import physics.components.ComponentClass
import physics.components.ComponentGroup
import physics.components.Field
import physics.computation.formulas.Formula
import physics.computation.ComponentRequirement
import physics.computation.Location
import physics.computation.databases.Database
import physics.computation.databases.DatabaseOptions
import physics.computation.formulas.FormulaOptions
import physics.computation.formulas.expressions.*
import physics.computation.others.ComplexKnowledge
import physics.values.units.PhysicalUnit
import physics.values.PhysicalDouble
import physics.values.PhysicalInt
import physics.values.PhysicalString
import java.io.File
import java.lang.StringBuilder
import kotlin.math.min


interface MutableNonNullableMap<K, V> {
    operator fun get(key: K): V
    operator fun set(k: K, value: V)
}


fun main() {
    PhysicalUnit.addConverter("g", "kg", 1.0 / 1000)
    PhysicalUnit.addConverter("mL", "L", 1.0 / 1000)
    PhysicalUnit.addConverter("mm", "m", 1.0 / 1000)
    PhysicalUnit.addConverter("ms", "s", 1.0 / 1000)
    PhysicalUnit.addAlias("N", mapOf("kg" to 1, "m" to 1, "s" to -2))

    // TODO : Avoid variables crashes when merging formulas together, and maybe change the way adaptable variables are
    //  handled

    val constants = ComponentClass("Constants",
        fieldsTemplates = listOf(
            Field.Template("G", PhysicalDouble.withUnit("N.kg-2.m2")),
            Field.Template("g", PhysicalDouble.withUnit("N.kg-1")),
        )
    ).invoke(
        fieldValuesAsStrings = mapOf(
            "G" to "6.67*10^-11",
            "g" to "9.81",
        )
    )

    val VolumeOwner = ComponentClass(
        "VolumeOwner",
        abstract = true,
        fieldsTemplates = listOf(
            Field.Template("volume", notation = "V", PhysicalDouble.withUnit("L")),
        ),
    )

    val Solvant = ComponentClass("Solvant", extends = listOf(VolumeOwner))
    val Solute = ComponentClass("Solute",
        extends = listOf(VolumeOwner),

        fieldsTemplates = listOf(
            Field.Template("masse", "m", PhysicalDouble.withUnit("kg")),
            Field.Template("masse volumique", "p", PhysicalDouble.withUnit("kg.L-1")),
        )
    )

    val Solution = ComponentClass("Solution",
        fieldsTemplates = listOf(
            Field.Template("masse", "m", PhysicalDouble.withUnit("kg")),
            Field.Template("volume", "V(?)|V", PhysicalDouble.withUnit("L")),
        ),
        subcomponentsGroupsTemplates = listOf(
            ComponentGroup.Template("solutés", contentType = Solute),
            ComponentGroup.Template("solvant", contentType = Solvant, minimumSize = 1, maximumSize = 1)
        ),
        representationField = "masse"
    )

    val Atome = ComponentClass(
        "Atome",
        fieldsTemplates = listOf(
            Field.Template("nom", PhysicalString.any()),
            Field.Template("Z", PhysicalInt),
            Field.Template("configuration électronique", PhysicalString.model { it matches Regex("(\\(\\d[spdf]\\)\\d{1,2})+") }),
        )
    )

    val formula1 = Formula(
        name = "Volume = somme des volumes des constituants",

        ComponentRequirement.single("S", type = Solution, location = Location.Any, variables = mapOf("Vs" to "volume")),
        ComponentRequirement.single("Sol", type = Solvant, location = Location.At("S.solvant"), variables = mapOf("Vsol" to "volume")),
        ComponentRequirement.single("X", type = Solute, location = Location.At("S.solutés"), variables = emptyMap()),
        ComponentRequirement.allRemaining("A#", type = Solute, location = Location.At("S.solutés"), variables = mapOf("Va#" to "volume")),

        output = "Vx" to Location.At("X.volume"),
        expression = "Vx" equal Var("Vs") - (Var("Vsol") + All("Va#", collectorName = "sum")),

        options = FormulaOptions.Implicit
    )

    val formula2 = Formula(
        "Masse volumique en fonction de la masse et du volume",

        ComponentRequirement.single("S", type = Solution, location = Location.Any, variables = mapOf("V" to "volume")),
        ComponentRequirement.single("X", type = Solute, location = Location.At("S.solutés"), variables = mapOf("m" to "masse")),

        output = "p" to Location.At("X.masse volumique"),
        expression = "p" equal Var("m") / Var("V"),
    )


    val tbl = Database(
        "Tableau périodique des éléments",

        options = DatabaseOptions.CASE_INSENSITIVE + DatabaseOptions.NORMALIZE,

        from = "PeriodicTableOfElements.csv",
        given = Atome,
        thenLink = mapOf(
            "nom" to "Element",
            "Z" to "AtomicNumber",
        )
    )

    fun oxydoreductionBetweenTheseExists(first: String, second: String): Boolean = true


    /*
    val oxydoreduction = Reaction(
        "Oxdoréduction",

        ComponentRequirement.single("S", type = Solution, location = Location.Any, variables = emptyMap()),
        ComponentRequirement.single("X", type = Atome, location = Location.At("S.solutés"), variables = mapOf("xName" to "name")),
        ComponentRequirement.single("Y", type = Atome, location = Location.At("S.solutés"), variables = mapOf("yName" to "name"), condition = {
            component, components ->
            oxydoreductionBetweenTheseExists(component.name, components.getValue("X").name)
        }),

        modified = { components: MutableNonNullableMap<String, Component> ->
            val (newX, newY) = oxydoreductionReact(components["X"], components["Y"])

            components["S"] = components["S"] {
                modify("solutés") {
                    - components["X"]
                    - components["Y"]
                    + newX
                    + newY
                }

                field("hello") .. PhysicalString("yo")
            }
        },

        trace = ReactionTrace(...)  // TODO
    )
     */

    val myComplexKnowledge = ComplexKnowledge(
        "Lecture et écriture d'une configuration électronique",

        ComponentRequirement.single("X", type = Atome, location = Location.Any, variables = mapOf("Z" to "Z")),
        output = "config" to Location.At("X.configuration électronique"),

        mappers = mapOf(
            "config" to mapper@{ args ->
                var remainingElectrons = (args["Z"] as PhysicalInt).value
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

                return@mapper PhysicalString(config.toString())
            },

            "Z" to mapper@{ args ->
                val config = args["config"] as PhysicalString
                return@mapper PhysicalInt(config.split(")").sumOf { it.split("(").first().ifEmpty { "0" }.toInt() })
            }
        )
    )

    val knowledge = listOf(formula1, formula2, tbl, myComplexKnowledge)

    val solvant = Solvant(fieldValuesAsStrings = mapOf("volume" to "Ve = 0.70 L"))
    val solute1 = Solute(fieldValuesAsStrings = mapOf("volume" to "0.20 L", "masse" to "20 g"))
    val solute2 = Solute(fieldValuesAsStrings = mapOf("volume" to "0.30 L", "masse volumique" to "20 g.L-1"))
    val solute3 = Solute(fieldValuesAsStrings = mapOf("volume" to "0.30 L"))
    val solutes = listOf(solute1, solute2, solute3)
    val solution = Solution(subcomponentGroupsContents = mapOf("solutés" to solutes, "solvant" to listOf(solvant)))
    val atome = Atome(fieldValuesAsStrings = mapOf("Z" to "29"))
    val componentClasses = mapOf("Solution" to Solution, "Soluté" to Solute, "Solvant" to Solvant, "Atome" to Atome)

    solution.fillFieldsWithTheirValuesUsing(knowledge)
    atome.fillFieldsWithTheirValuesUsing(knowledge)

    println(solution)
    println()
    println(atome)

    KnowledgeLoader(componentClasses).loadFrom(File("${cwd()}\\resources\\formulas.data"))
}

