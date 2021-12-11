package physics.components

import physics.values.PhysicalValue
import println


class FlexibleRequirementsHandler(
    requirements: List<ComponentRequirement>,
    output: Pair<String, Location.At>
) : RequirementsHandler(requirements) {

    constructor(
        vararg requirements: ComponentRequirement,
        output: Pair<String, Location.At>
    ): this(requirements.toList(), output)

    val outputVariable = output.first
    private val requirementCorrespondingToOutput = this.requirements.single { it.alias == output.second.alias }
    private val outputLocation = output.second

    fun getArguments(
        system: PhysicalSystem,
        outputOwner: Component
    ): Map<String, PhysicalValue<*>> {
        return super.getArguments(system, mapOf(outputLocation.alias to outputOwner))
    }

    fun getRequiredFields(
        system: PhysicalSystem,
        outputOwner: Component
    ): Map<String, Field<*>> {
        return super.getRequiredFields(system, mapOf(outputLocation.alias to outputOwner))
    }

    fun selectRequiredComponentsIn(
        system: PhysicalSystem,
        outputOwner: Component
    ): Map<String, Component> {
        return super.selectRequiredComponentsIn(system, mapOf(outputLocation.alias to outputOwner))
    }

    fun findVariableCorrespondingTo(field: String, owner: Component): String? {
        if (field == outputLocation.field && owner instanceOf requirementCorrespondingToOutput.type) return outputVariable

        for (requirement in requirements.filter { !it.selectAll && owner instanceOf it.type }) {
            for ((variable, backingField) in requirement.ownedVariables) {
                if (backingField == field) return variable
            }
        }

        return null
    }

    fun isolateVariable(variable: String): FlexibleRequirementsHandler {
        if (variable == outputVariable) return this

        val oldOutputRequirement = requirementCorrespondingToOutput
        val newOutputRequirement = requirements.single { variable in it.ownedVariables }
        val matchingField = newOutputRequirement.ownedVariables.getValue(variable)

        return FlexibleRequirementsHandler(
            requirements.map { requirement -> when {
                requirement === oldOutputRequirement && requirement === newOutputRequirement -> requirement.withRequiredVariable(outputVariable, outputLocation.field).withOptionalField(matchingField)
                requirement === oldOutputRequirement -> requirement.withRequiredVariable(outputVariable, outputLocation.field)
                requirement === newOutputRequirement -> requirement.withOptionalField(matchingField)
                else -> requirement
            } },
            variable to Location.At(oldOutputRequirement.alias, matchingField)
        )
    }

    private fun ownerOf(requirement: ComponentRequirement, where: List<ComponentRequirement>) =
        when (requirement.location) {
            is Location.At -> where.single { it.alias == requirement.location.alias }
            is Location.Any -> null
        }

    private fun updateAliasesToAvoidNameCrashes(
        requirements: List<ComponentRequirement>,
        conflictingRequirements: List<ComponentRequirement>,
        _aliasesLinkedToTheirNumericalSuffixes: Map<String, Int> = emptyMap()
    ) : Pair<Map<String, String>, Map<String, Int>> {

        fun String.suppressNumericalSuffix(): String {
            return if (this.last().isDigit() && this.dropLastWhile { it.isDigit() }.endsWith("-")) {
                this.dropLastWhile { it.isDigit() }.removeSuffix("-")
            } else this
        }

        val oldAliasesLinkedToNewOnesInThatFormula = requirements.map { it.alias }.associateWithTo(mutableMapOf()) { it }
        val aliasesInUseInOtherFormula = conflictingRequirements.map { it.alias.suppressNumericalSuffix() }
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

    fun composedWith(other: FlexibleRequirementsHandler, joiningVariable: String): FlexibleRequirementsHandler {
        val (oldAliasLinkedToNewOnes, aliasesLinkedToTheirNumericalSuffixes) = updateAliasesToAvoidNameCrashes(requirements, other.requirements)
        val (oldConflictingAliasesLinkedToNewOne, _) = updateAliasesToAvoidNameCrashes(other.requirements, requirements, aliasesLinkedToTheirNumericalSuffixes)
        val renamedRequirementsOfThatFormula = requirements.map { it.withAliasesReferencesUpdated(oldAliasLinkedToNewOnes) }
        val renamedRequirementsOfPluggedFormula = other.requirements.map { it.withAliasesReferencesUpdated(oldConflictingAliasesLinkedToNewOne) }

        val requirementsToBeFusedTogether = mutableListOf(renamedRequirementsOfThatFormula.single { joiningVariable in it.ownedVariables } to renamedRequirementsOfPluggedFormula.single { it.alias == oldConflictingAliasesLinkedToNewOne[other.requirementCorrespondingToOutput.alias] } )
        while (true) {
            val (requirementOnThisSide, requirementOnPluggedSide) = requirementsToBeFusedTogether.last()
            if (requirementOnThisSide.canBeLocatedAnywhere || requirementOnPluggedSide.canBeLocatedAnywhere) break
            requirementsToBeFusedTogether.add(ownerOf(requirementOnThisSide, renamedRequirementsOfThatFormula)!! to ownerOf(requirementOnPluggedSide, renamedRequirementsOfPluggedFormula)!!)
        }

        val remainingRequirementsOfThatFormula = renamedRequirementsOfThatFormula.toMutableList()
        val remainingRequirementsOfPluggedFormula = renamedRequirementsOfPluggedFormula.toMutableList()
        for ((requirement1, requirement2) in requirementsToBeFusedTogether) {
            remainingRequirementsOfThatFormula.remove(requirement1)
            remainingRequirementsOfPluggedFormula.remove(requirement2)
            remainingRequirementsOfPluggedFormula.replaceAll { it.withAliasReferenceUpdated(requirement2.alias, requirement1.alias) }  // Preparing fusion
        }

        val newRequirements = (
                remainingRequirementsOfThatFormula +
                remainingRequirementsOfPluggedFormula +
                requirementsToBeFusedTogether.map { (r1, r2) -> r1.fuseWith(r2).withOptionalVariable(other.outputVariable) }
        )

        return FlexibleRequirementsHandler(
            newRequirements,
            outputVariable to renamedRequirementsOfThatFormula.single { outputVariable in it.ownedVariables }.location as Location.At
        )
    }
}
