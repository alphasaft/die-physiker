

fun main() {
    PhysicalDoubleTests.runAll()

    PhysicalUnit.addConverter("g", "kg", 1.0 / 1000)
    PhysicalUnit.addConverter("mL", "L", 1.0 / 1000)
    PhysicalUnit.addConverter("mm", "m", 1.0 / 1000)
    PhysicalUnit.addConverter("mACC", "ACC", 1.0 / 1000)
    PhysicalUnit.addAlias("ACC", mapOf("m" to 1, "s" to -2))

    fun unit(unit: String) = PhysicalDouble.withUnit(unit)

    val Solute = ComponentClass(
        "Solute",
        extends = emptyList(),
        fieldsTemplates = listOf(
            Field.Template("masse", unit("kg")),
            Field.Template("masse volumique", unit("kg.L-1"))
        ),
        subcomponentsGroupsTemplates = emptyList()
    )

    val Solvant = ComponentClass("Solvant")

    val Solution = ComponentClass(
        "Solution",
        fieldsTemplates = listOf(
            Field.Template("volume", unit("L")),
            Field.Template("PH", PhysicalInt)
        ),
        subcomponentsGroupsTemplates = listOf(
            ComponentGroup.Template("solutés", Solute),
            ComponentGroup.Template("solvant", Solvant, minimumSize = 0, maximumSize = 1)
        ),
    )

    val Atom = ComponentClass(
        "Atom",
        fieldsTemplates = listOf(
            Field.Template("nom", PhysicalString.any()),
            Field.Template("Z", PhysicalInt),
            Field.Template("config", PhysicalString.model { it matches Regex("(\\(?\\d[spdl]\\)?\\d+,?\\s*)+") }),
        )
    )

    val s1 = Solute(mapOf("masse volumique" to "2.0 g.L-1", "masse" to "3.0 g"))
    val s2 = Solute(mapOf())

    val mySolution = Solution(
        fieldValuesAsStrings = mapOf(),
        subcomponentGroupsContents = mapOf("solutés" to listOf(s1, s2))
    )

    val f: PhysicalRelationship = Formula(
        "Masse volumique en fonction de la masse et du volume",

        Requirement.single("S", a = Solution, from = null, withVariables = mapOf("V" to "volume")),
        Requirement.single("X", a = Solute, from = "S.solutés", withVariables = mapOf("m" to "masse")),

        // output = "p" to "X.masse volumique",
        variables = emptyList(),

        expression = "p" equal Var("m") / Var("V")
    )

    val connection = RuntimeDatabaseConnection(
        columns = listOf("nom", "Z"),
        lines = listOf(
            listOf("Hydrogène", "1"),
            listOf("Hélium", "2")
        )
    )
    val database = Database("Le tableau périodique des elements", connection)


    // 1s22s1
    // OU (1s)2(2s)1
    // OU 1s2, 2s1
    // OU (1s)2, (2s)1

    val myAtom = Atom(mapOf("Z" to "1"))
    val system = PhysicalSystem(myAtom)
    val field = myAtom.getField<PhysicalString>("nom")
    println(field)

    // TODO : Simply associate words and blah blah blah with concrete components implementation
}
