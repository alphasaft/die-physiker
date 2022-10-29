@file:Suppress("LocalVariableName")

import physics.components.*
import physics.packaging.startCompilation
import physics.quantities.PDouble
import physics.quantities.PInt
import physics.quantities.PString
import physics.quantities.expressions.div
import physics.quantities.expressions.equals
import physics.quantities.expressions.v
import physics.rules.*


fun main() {

    val compilation = startCompilation()



    compilation.module("base") {

        ("Equation") .. ComponentClass(
            "Equation",
            structure = ComponentStructure(
                fieldsTemplates = mapOf(
                    "solutions" to Field.Template<PDouble>("solutions", notation = Field.Template.Notation.Always("S"))
                ),
                stateEquationsTemplates = mapOf(
                    "E" to StateEquation.Template("x")
                )
            )
        )



        ("List") .. ComponentClass(
            "List",
            structure = ComponentStructure(
                boxesTemplates = mapOf(
                    "items" to ComponentBox.Template("items", ComponentClass.Any)
                )
            )
        )
    }




    compilation.module("chemistry") {

        useQualified("base")

        val periodicTableOfElements = Database("Tableau Périodique", CsvDatabaseReader("${cwd()}\\resources\\PeriodicTableOfElements.csv"))

        ("Atome") .. ComponentClass(
            "Atome",
            structure = ComponentStructure(
                fieldsTemplates = mapOf(
                    "nom" to Field.Template<PString>("nom", Field.Template.Notation.Always("nom")),
                    "numéro atomique" to Field.Template<PInt>("numéro atomique", Field.Template.Notation.Always("Z"))
                )
            ),
            rules = setOf(
                Query(
                    NameRootComponent("A"),
                    ExtractField("AtomicNumber", sourceIdentifier = "A", fieldName = "numéro atomique"),
                    ExtractField("Element", sourceIdentifier = "A", fieldName = "nom")
                ).let { ForEachQueryResult(it) perform ApplyRelation(periodicTableOfElements) }
            )
        )

        ("Liquide") .. ComponentClass(
            "Liquide",
            abstract = true,
            structure = ComponentStructure(
                fieldsTemplates = mapOf(
                    "volume" to Field.Template<PDouble>("volume", Field.Template.Notation.UseUnderscore("V"))
                )
            )
        )

        ("Solute") .. ComponentClass(
            "Soluté",
            structure = ComponentStructure(
                fieldsTemplates = mapOf(
                    "masse" to Field.Template<PDouble>("masse", Field.Template.Notation.UseUnderscore("m")),
                    "masse volumique" to Field.Template<PDouble>("masse volumique", Field.Template.Notation.UseParenthesis("p"))
                )
            )
        )

        ("Solvant") .. ComponentClass(
            "Solvant",
            structure = ComponentStructure(
                extends = setOf("Liquide"()),
                fieldsTemplates = mapOf(
                    "nom" to Field.Template<PString>("nom", Field.Template.Notation.Always("nom"))
                )
            )
        )

        ("Solution") .. ComponentClass(
            "Solution",
            structure = ComponentStructure(
                extends = setOf("Liquide"()),
                boxesTemplates = mapOf(
                    "solutés" to ComponentBox.Template("solutés", "Solute"()),
                    "solvant" to ComponentBox.Template("solvant", "Solvant"())
                )
            ),
            rules = setOf(
                Query(
                    NameRootComponent("S"),
                    SelectComponent("Sol", sourceIdentifier = "S", boxName = "solvant"),
                    ExtractField("Vs", sourceIdentifier = "S", fieldName = "volume"),
                    ExtractField("VSol", sourceIdentifier = "Sol", fieldName = "volume")
                ).let { ForEachQueryResult(it) perform ApplyRelation(Formula(v("Vs") equals v("VSol"))) },
                Query(
                    NameRootComponent("S"),
                    SelectComponent("X", sourceIdentifier = "S", boxName = "solutés"),
                    ExtractField("p", sourceIdentifier = "X", fieldName = "masse volumique"),
                    ExtractField("m", sourceIdentifier = "X", fieldName = "masse"),
                    ExtractField("V", sourceIdentifier = "S", fieldName = "volume")
                ).let { ForEachQueryResult(it) perform ApplyRelation(Formula(v("p") equals v("m") / v("V"))) }
            )
        )
    }



    val chemistry = compilation.asModule()
    val Solute = chemistry["Solute"]
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
    print(p.toStringWithHistory())
}
