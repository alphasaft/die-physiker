import physics.components.ComponentClass
import physics.components.ComponentGroup
import physics.components.Field
import physics.components.PhysicalSystem
import physics.computation.formulas.Formula
import physics.computation.ComponentRequirement
import physics.computation.databases.Database
import physics.computation.formulas.expressions.*
import physics.computation.others.ComplexKnowledge
import physics.units.PhysicalUnit
import physics.values.PhysicalDouble
import physics.values.PhysicalInt
import physics.values.PhysicalString
import physics.values.PhysicalValue


fun main() {
    PhysicalUnit.addConverter("g", "kg", 1.0 / 1000)
    PhysicalUnit.addConverter("mL", "L", 1.0 / 1000)
    PhysicalUnit.addConverter("mm", "m", 1.0 / 1000)
    PhysicalUnit.addConverter("mACC", "ACC", 1.0 / 1000)
    PhysicalUnit.addAlias("N", mapOf("kg" to 1, "m" to 1, "s" to -2))

    // TODO : Simply associate words and blah blah blah with concrete components implementation
    // TODO : Implement proxies ?


    val Constants = ComponentClass("Constants",
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
            Field.Template("volume", PhysicalDouble.withUnit("L")),
        )
    )

    val Solvant = ComponentClass("Solvant", extends = listOf(VolumeOwner))
    val Solute = ComponentClass("Solute",
        extends = listOf(VolumeOwner),

        fieldsTemplates = listOf(
            Field.Template("masse", PhysicalDouble.withUnit("kg")),
            Field.Template("masse volumique", PhysicalDouble.withUnit("kg.L-1")),
        )
    )

    val Solution = ComponentClass("Solution",
        fieldsTemplates = listOf(
            Field.Template("masse", PhysicalDouble.withUnit("kg")),
            Field.Template("volume", PhysicalDouble.withUnit("L")),
        ),
        subcomponentsGroupsTemplates = listOf(
            ComponentGroup.Template("solutés", contentType = Solute),
            ComponentGroup.Template("solvant", contentType = Solvant, minimumSize = 1, maximumSize = 1)
        )
    )

    val Atome = ComponentClass(
        "Atome",
        fieldsTemplates = listOf(
            Field.Template("nom", PhysicalString.any()),
            Field.Template("Z", PhysicalInt),
            Field.Template("configuration électronique", PhysicalString.any()),
        )
    )

    val formula1 = Formula(
        name = "volume truc",

        ComponentRequirement.single("S", type = Solution, location = null, variables = mapOf("Vs" to "volume")),
        ComponentRequirement.single("Sol", type = Solvant, location = "S.solvant", variables = mapOf("Vsol" to "volume")),
        ComponentRequirement.single("X", type = Solute, location = "S.solutés", variables = emptyMap()),
        ComponentRequirement.allRemaining("A#", type = Solute, location = "S.solutés", variables = mapOf("Va#" to "volume")),

        output = "Vx" to "X.volume",
        expression = "Vx" equal Var("Vs") - (Var("Vsol") + AllVars("Va#", collectorName = "sum")),

        implicit = true
    )

    val formula2 = Formula(
        "Masse volumique en fonction de la masse et du volume",

        ComponentRequirement.single("S", type = Solution, location = null, variables = mapOf("V" to "volume")),
        ComponentRequirement.single("X", type = Solute, location = "S.solutés", variables = mapOf("m" to "masse")),

        output = "p" to "X.masse volumique",
        expression = "p" equal Var("m") / Var("V")
    )


    val tbl = Database(
        "Tableau périodique",

        from = "PeriodicTableOfElements.csv",
        given = Atome,
        thenLink = mapOf(
            "nom" to "Element",
            "Z" to "AtomicNumber",
        )
    )

    val myComplexKnowledge = ComplexKnowledge(
        "Zebi",

        ComponentRequirement.single("X", type = Atome, location = null, variables = mapOf("Z" to "Z")),
        output = "c" to "X.configuration électronique",

        mappers = mapOf(
            "c" to mapper@{ args ->
                val Z = args["Z"] as PhysicalInt
                return@mapper if (Z.value % 2 == 0) PhysicalString("even") else PhysicalString("odd")
            },
            "Z" to mapper@{ args ->
                val c = args["c"] as PhysicalString
                return@mapper PhysicalInt(if (c.value == "odd") 1 else 2)
            }
        )
    )

    val solvant = Solvant(fieldValuesAsStrings = mapOf("volume" to "0.70 L"))
    val solute1 = Solute(fieldValuesAsStrings = mapOf("volume" to "0.20 L", "masse" to "20 g"))
    val solute2 = Solute(fieldValuesAsStrings = mapOf("volume" to "0.30 L"))
    val solute3 = Solute(fieldValuesAsStrings = mapOf("volume" to "0.30 L"))
    val solutes = listOf(solute1, solute2, solute3)
    val solution = Solution(subcomponentGroupsContents = mapOf("solutés" to solutes, "solvant" to listOf(solvant)))
    val system1 = PhysicalSystem(solution)

    val atome = Atome(fieldValuesAsStrings = mapOf("configuration électronique" to "even"))
    val system2 = PhysicalSystem(atome)

    formula1.fillFieldWithItsValue(solution.getField("volume"), system = system1)
    formula2.fillFieldWithItsValue(solute1.getField("masse volumique"), system = system1)
    println(solute1.getField("masse volumique"))

    println(myComplexKnowledge.getFieldValue(atome.getField("Z"), system2))

    /*
    fun getPrime(n: Int): Int {
        val primes = mutableListOf(2, 3)
        while (primes.size < n) {
            var nextPrime = primes.last() + 2
            while (true) {
                if (primes.none { nextPrime % it == 0 }) {
                    primes.add(nextPrime)
                    break
                } else nextPrime += 2
            }
        }
        return primes.last()
    }

    println(getPrime(100))

     */

    // TODO : Comment faire un Rubik's cube ?

}
