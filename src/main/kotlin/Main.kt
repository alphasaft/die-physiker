@file:Suppress("LocalVariableName")

import physics.components.*
import physics.packaging.startCompilation
import physics.quantities.*
import physics.quantities.PFunction
import physics.quantities.expressions.*
import physics.rules.*
import physics.rules.relations.CsvDatabaseReader
import physics.rules.relations.Database
import physics.rules.relations.Formula


fun main() {

    fun findZeroOf(f: PFunction, x0: PDouble, epsilon: PDouble): Double {
        val fp = f.derivative
        var x = x0
        while (f(x).abs() >= epsilon) {
            x -= f(x) / fp(x)
        }
        return x.toDouble().roundAt(-3)
    }

    val f = (exp(v("x"))).."x"
    println(findZeroOf(f, x0 = PDouble(1), epsilon = PDouble(0.00000000000001)))

    val I = PDoubleInterval.raw(
        isLowerBoundClosed = true,
        lowerBound = PDouble(-1.0),
        upperBound = PDouble(1.0),
        isUpperBoundClosed = true,
    )

    val compilation = startCompilation()


    compilation.module("base") {

        + ComponentClass(
            "Equation",
            structure = ComponentStructure(
                fieldsTemplates = listOf(
                    Field.Template<PDouble>("solutions", notation = Field.Template.Notation.Always("S"))
                ),
                stateEquationsTemplates = mapOf(
                    "E" to StateEquation.Template("x")
                )
            )
        )

        + ComponentClass(
            "IntBox",
            structure = ComponentStructure(
                fieldsTemplates = listOf(
                    Field.Template<PInt>("value", Field.Template.Notation.Always("value"))
                )
            )
        )

        + ComponentClass(
            "List",
            structure = ComponentStructure(
                boxesTemplates = listOf(
                    ComponentBox.Template("items", "IntBox"())
                )
            )
        )
    }


    compilation.module("chemistry") {

        val periodicTableOfElements = Database("Tableau Périodique", CsvDatabaseReader("${cwd()}\\resources\\PeriodicTableOfElements.csv"))

        + ComponentClass(
            "Atome",
            structure = ComponentStructure(
                fieldsTemplates = listOf(
                    Field.Template<PString>("nom", Field.Template.Notation.Always("nom")),
                    Field.Template<PInt>("numéro atomique", Field.Template.Notation.Always("Z"))
                )
            ),
            rules = setOf(
                ForEachQueryResult(Query(
                    NameRootComponent("A"),
                    ExtractField("AtomicNumber", sourceIdentifier = "A", fieldName = "numéro atomique"),
                    ExtractField("Element", sourceIdentifier = "A", fieldName = "nom")
                )) pleaseDo ApplyRelation(periodicTableOfElements)
            )
        )

        + ComponentClass(
            "Liquide",
            abstract = true,
            structure = ComponentStructure(
                fieldsTemplates = listOf(
                    Field.Template<PDouble>("volume", Field.Template.Notation.UseUnderscore("V"), WithUnit("L"))
                )
            )
        )

        + ComponentClass(
            "Soluté",
            structure = ComponentStructure(
                fieldsTemplates = listOf(
                    Field.Template<PDouble>("masse", Field.Template.Notation.UseUnderscore("m")),
                    Field.Template<PDouble>("masse volumique", Field.Template.Notation.UseParenthesis("p"))
                )
            )
        )

        + ComponentClass(
            "Solvant",
            structure = ComponentStructure(
                extends = setOf("Liquide"()),
                fieldsTemplates = listOf(
                    Field.Template<PString>("nom", Field.Template.Notation.Always("nom"))
                )
            )
        )

        + ComponentClass(
            "Solution",
            structure = ComponentStructure(
                extends = setOf("Liquide"()),
                boxesTemplates = listOf(
                    ComponentBox.Template("solutés", "Soluté"()),
                    ComponentBox.Template("solvant", "Solvant"())
                )
            ),
            rules = setOf(
                ForEachQueryResult(Query(
                    NameRootComponent("S"),
                    SelectComponent("Sol", sourceIdentifier = "S", boxName = "solvant"),
                    ExtractField("Vs", sourceIdentifier = "S", fieldName = "volume"),
                    ExtractField("VSol", sourceIdentifier = "Sol", fieldName = "volume")
                )) pleaseDo ApplyRelation(Formula(v("Vs") equals v("VSol"))),
                ForEachQueryResult(Query(
                    NameRootComponent("S"),
                    SelectComponent("X", sourceIdentifier = "S", boxName = "solutés"),
                    ExtractField("p", sourceIdentifier = "X", fieldName = "masse volumique"),
                    ExtractField("m", sourceIdentifier = "X", fieldName = "masse"),
                    ExtractField("V", sourceIdentifier = "S", fieldName = "volume")
                )) pleaseDo ApplyRelation(Formula(v("p") equals v("m") / v("V")))
            )
        )
    }



    val chemistry = compilation.asModule()
    val Solute = chemistry["Soluté"]
    val Solvant = chemistry["Solvant"]
    val Solution = chemistry["Solution"]
    val Atome = chemistry["Atome"]

    val solute1 = Solute("Solu1", fieldValues = mapOf("masse" to PDouble("10 g"), "masse volumique" to PDouble("10.1 g.L-1")))
    val solute2 = Solute("Solu2", fieldValues = mapOf("masse" to PDouble("5 g")))
    val solvant = Solvant("Solv", fieldValues = mapOf("nom" to PString("eau")))
    val s = Solution(
        "S",
        fieldValues = mapOf(),
        boxesContents = mapOf(
            "solutés" to listOf(solute1, solute2),
            "solvant" to listOf(solvant),
        )
    ).apply { update() }

    val a = Atome(
        "A",
        fieldValues = mapOf(
            "nom" to PString("Hélium"),
        ),
    ).apply { update() }

    val p = solute2.getField("masse volumique")
    printAll(p.formatHistory())
}
