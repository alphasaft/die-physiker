import physics.*
import physics.specs.ComponentSpec
import physics.specs.FieldAccessSpec
import physics.specs.FieldSpec
import physics.specs.RootComponentSpec
import util.println


fun main() {

    // TODO : Write Formulas parser, but only after you did the thing above
    // TODO : Simply associate words and blah blah blaaaah with concrete components implementation

    // Eh that's quite verbose but all is well encapsulated AND that wasn't done to be written smoothly, but to be
    // loaded from a data file, so : verbosity and clearness of what's happening over beauty.
    PhysicalComponentModel.addFormulas(generateFormulas(
        rootSpec = RootComponentSpec("Solution", "S"),
        componentsSpecs = listOf(ComponentSpec("S.solutes", "X"), ComponentSpec("S.solutes", "A", select='+')),
        concernedFields = listOf(FieldAccessSpec("S.mass"), FieldAccessSpec("A.mass"), FieldAccessSpec("X.mass")),
        expressionsDependingOnRequiredOutput = mapOf(
            "X.mass" to { args -> args.get<Double>("S.mass") - args.getAll<Double>("A.mass").sum() },
            "S.mass" to { args -> args.get<Double>("X.mass") + args.getAll<Double>("A.mass").sum()  }
        )
    ))

    PhysicalComponentModel.addFormulas(generateFormulas(
        rootSpec = RootComponentSpec("Solution", "S"),
        componentsSpecs = listOf(ComponentSpec("S.solutes", "X")),
        concernedFields = listOf(FieldAccessSpec("S.volume"), FieldAccessSpec("X.mass"), FieldAccessSpec("X.density")),
        expressionsDependingOnRequiredOutput = mapOf(
            "X.mass" to { args -> args.get<Double>("X.density") * args.get<Double>("S.volume") },
            "X.density" to { args -> args.get<Double>("X.mass") / args.get<Double>("S.volume") },
            "S.volume" to { args -> args.displayStorage() ; args.get<Double>("X.mass") / args.get<Double>("X.density") }
        )
    ))

    /* TODO : Couldn't we do something like
        Formula "name" for Solution S needs X from S.solutes, A+ from S.solutes, S.mass, A.mass and returns X.mass = S.mass - sum(A.mass*)
        That's far from impossible */
    // TODO : I was thinking about doing the same, but for the units, instead of a verbose XML ?

    val Solution = RootPhysicalComponentModel(
        name = "Solution",
        fieldSpecs = listOf(FieldSpec("mass", Double::class), FieldSpec("volume", Double::class)),
        subComponentsNames = mapOf("solutes" to "Solute", "solvent" to "Liquid"),
    )

    val Solute = PhysicalComponentModel(
        name = "Solute",
        fieldsSpecs = listOf(FieldSpec("mass", Double::class), FieldSpec("density", Double::class)),
    )

    val Liquid = PhysicalComponentModel(
        name = "Liquid",
        fieldsSpecs = listOf(FieldSpec("volume", Double::class))
    )

    val solute = Solute("mass" to 10.0, "density" to 5.0)
    val solute2 = Solute("mass" to 10.0)
    val solute3 = Solute()//"density" to 10.0)
    val solution = Solution(subComponents = mapOf("solutes" to listOf(solute, solute2, solute3)))
    println(solution.knownFields, solution.getSubComponents("solutes").map { it.knownFields })
}
