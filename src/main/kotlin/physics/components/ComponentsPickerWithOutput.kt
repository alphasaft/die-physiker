package physics.components

import physics.quantities.PValue
import physics.quantities.Quantity


class ComponentsPickerWithOutput(
    pickers: List<ComponentSpec>,
    output: Pair<String, Location.At>
) : ComponentsPicker(pickers) {

    constructor(
        vararg pickers: ComponentSpec,
        output: Pair<String, Location.At>
    ): this(pickers.toList(), output)

    val outputVariable = output.first
    private val outputLocation = output.second
    private val specCorrespondingToOutput = this.specs.single { it.alias == outputLocation.alias }

    fun pickRequiredFields(
        context: Context,
        outputOwner: Component
    ): Map<String, Field<*>> {
        return super.pickRequiredFields(context, mapOf(outputLocation.alias to outputOwner))
    }

    fun pickVariablesValues(
        context: Context,
        outputOwner: Component
    ): Map<String, Quantity<*>> {
        return super.pickVariableValues(context, mapOf(outputLocation.alias to outputOwner))
    }

    fun findVariableCorrespondingTo(field: Field<*>): String? {
        if (field.name == outputLocation.field && field.owner instanceOf specCorrespondingToOutput.type) return outputVariable
        for (spec in specs.filter { !it.selectAll && field.owner instanceOf it.type }) {
            for ((variable, backingField) in spec.ownedVariables) if (backingField == field.name) return variable
        }
        return null
    }

    fun isolateVariable(variable: String): ComponentsPickerWithOutput {
        if (variable == outputVariable) return this

        val newOutputSpec = specs.single { it.ownsVariable(variable) }
        val fieldCorrespondingToOutput = newOutputSpec.ownedVariables.getValue(variable)

        return ComponentsPickerWithOutput(
            specs.map { spec -> when {
                spec === specCorrespondingToOutput && spec === newOutputSpec -> spec.withRequiredVariable(outputVariable, outputLocation.field).withOptionalField(fieldCorrespondingToOutput)
                spec === specCorrespondingToOutput -> spec.withRequiredVariable(
                    outputVariable,
                    outputLocation.field
                )
                spec === newOutputSpec -> spec.withOptionalField(fieldCorrespondingToOutput)
                else -> spec
            } },
            variable to Location.At(newOutputSpec.alias, fieldCorrespondingToOutput)
        )
    }

    private fun ownerOf(picker: ComponentSpec, where: List<ComponentSpec>) =
        when (picker.location) {
            is Location.At -> where.single { it.alias == picker.location.alias }
            is Location.Any -> null
        }

    private fun updateAliasesToAvoidNameCrashes(
        pickers: List<ComponentSpec>,
        conflictingPickers: List<ComponentSpec>,
        _aliasesLinkedToTheirNumericalSuffixes: Map<String, Int> = emptyMap()
    ) : Pair<Map<String, String>, Map<String, Int>> {

        fun String.suppressNumericalSuffix(): String =
            if (this.last().isDigit() && this.dropLastWhile { it.isDigit() }.endsWith("-")) this.dropLastWhile { it.isDigit() }.removeSuffix("-")
            else this

        val oldAliasesLinkedToNewOnesInThatFormula = pickers.map { it.alias }.associateWithTo(mutableMapOf()) { it }
        val aliasesInUseInOtherFormula = conflictingPickers.map { it.alias.suppressNumericalSuffix() }
        val aliasesLinkedToTheirNumericalSuffixes = _aliasesLinkedToTheirNumericalSuffixes.toMutableMap()

        for (alias in oldAliasesLinkedToNewOnesInThatFormula.keys) {
            val withNumericalSuffixRemoved = alias.suppressNumericalSuffix()
            if (withNumericalSuffixRemoved in aliasesInUseInOtherFormula) {
                val numericalSuffix = aliasesLinkedToTheirNumericalSuffixes[withNumericalSuffixRemoved] ?: 1
                oldAliasesLinkedToNewOnesInThatFormula[alias] = "$alias-$numericalSuffix"
                aliasesLinkedToTheirNumericalSuffixes[alias] = numericalSuffix + 1
            }
        }

        return oldAliasesLinkedToNewOnesInThatFormula to aliasesLinkedToTheirNumericalSuffixes.toMap()
    }

    fun composedWith(other: ComponentsPickerWithOutput, joiningVariable: String): ComponentsPickerWithOutput {
        val (oldAliasLinkedToNewOnes, aliasesLinkedToTheirNumericalSuffixes) = updateAliasesToAvoidNameCrashes(specs, other.specs)
        val (oldConflictingAliasesLinkedToNewOne, _) = updateAliasesToAvoidNameCrashes(other.specs, specs, aliasesLinkedToTheirNumericalSuffixes)
        val renamedSpecsOfThatFormula = specs.map { it.withAliasesReferencesUpdated(oldAliasLinkedToNewOnes) }
        val renamedSpecsOfPluggedFormula = other.specs.map { it.withAliasesReferencesUpdated(oldConflictingAliasesLinkedToNewOne) }

        val specsToBeFusedTogether = mutableListOf(renamedSpecsOfThatFormula.single { joiningVariable in it.ownedVariables } to renamedSpecsOfPluggedFormula.single { it.alias == oldConflictingAliasesLinkedToNewOne[other.specCorrespondingToOutput.alias] } )
        while (true) {
            val (specOnThisSide, specOnPluggedSide) = specsToBeFusedTogether.last()
            if (specOnThisSide.canBeLocatedAnywhere || specOnPluggedSide.canBeLocatedAnywhere) break
            specsToBeFusedTogether.add(ownerOf(specOnThisSide, renamedSpecsOfThatFormula)!! to ownerOf(specOnPluggedSide, renamedSpecsOfPluggedFormula)!!)
        }

        val remainingSpecsOfThatFormula = renamedSpecsOfThatFormula.toMutableList()
        val remainingSpecsOfPluggedFormula = renamedSpecsOfPluggedFormula.toMutableList()
        for ((spec1, spec2) in specsToBeFusedTogether) {
            remainingSpecsOfThatFormula.remove(spec1)
            remainingSpecsOfPluggedFormula.remove(spec2)
            remainingSpecsOfPluggedFormula.replaceAll { it.withAliasReferenceUpdated(spec2.alias, spec1.alias) }  // Preparing fusion
        }

        val newSpecs = (
                remainingSpecsOfThatFormula +
                remainingSpecsOfPluggedFormula +
                specsToBeFusedTogether.map { (r1, r2) -> r1.fuseWith(r2).withOptionalVariable(other.outputVariable) }
        )

        return ComponentsPickerWithOutput(
            newSpecs,
            outputVariable to renamedSpecsOfThatFormula.single { outputVariable in it.ownedVariables }.location as Location.At
        )
    }
}
