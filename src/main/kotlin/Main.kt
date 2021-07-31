import physics.components.*
import physics.formulas.*
import physics.formulas.expressions.*
import physics.units.PhysicalUnit
import physics.values.*
import kotlin.math.sqrt


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
            Field.Template("électrons", PhysicalInt),
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

        Requirement(null, Solution, listOf("volume"), "S"),
        Requirement("S.solutés", Solute, listOf("masse"), "X"),

        variables = listOf(
            FormulaVariable("p", "X.masse volumique"),
            FormulaVariable("m", "X.masse"),
            FormulaVariable("V", "S.volume"),
        ),

        expression = "p" equal Var("m") / Var("V")
    )

    val r: PhysicalRelationship = DataMapper(
        "Config. électronique en fonction du nombre d'électrons",

        Requirement(null, Atom, listOf("électrons"), "A"),

        variables = listOf(
            FormulaVariable("e", "A.électrons"),
            FormulaVariable("c", "A.config"),
        ),

        output = "c",

        mappers = mapOf(
            "c" to { args: Map<String, PhysicalValue<*>> -> args.values.first() },
            "e" to { args: Map<String, PhysicalValue<*>> -> PhysicalInt(args.values.first().toPhysicalString()[4].digitToInt()) }
        )
    )

    // 1s22s1
    // OU (1s)2(2s)1
    // OU 1s2, 2s1
    // OU (1s)2, (2s)1

    val myAtom = Atom(mapOf("config" to "(1s)1, (2p)2"))
    val system = PhysicalSystem(myAtom)
    val field = myAtom.getField<PhysicalInt>("électrons")
    r.fillFieldWithItsValue(field, system)
    println(field)

    // TODO : Simply associate words and blah blah blah with concrete components implementation
}
