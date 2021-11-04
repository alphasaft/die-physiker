package physics.computation.formulas

import mergedWith
import physics.KnowledgeException
import physics.InappropriateKnowledgeException
import physics.VariableNameCrashError
import physics.components.Component
import physics.components.Field
import physics.components.PhysicalSystem
import physics.computation.ComponentRequirement
import physics.computation.AbstractPhysicalKnowledge
import physics.computation.formulas.expressions.Equality
import physics.values.*


class Formula(
    private val obtentionMethod: ObtentionMethod,
    private val implicit: Boolean,
    requirements: List<ComponentRequirement>,
    output: Pair<String, String>,
    val expression: Equality,
): AbstractPhysicalKnowledge(
    requirements,
    output
) {
    constructor(
        name: String,
        vararg requirements: ComponentRequirement,
        output: Pair<String, String>,
        expression: Equality,
        implicit: Boolean = true,
    ): this(
        ObtentionMethod.Builtin(name),
        implicit,
        requirements.toList().map { it.withFollowingOverlappingAliasesForbidden(requirements.mapTo(mutableSetOf()) { c -> c.alias }) },
        output,
        expression,
    )

    sealed class ObtentionMethod {
        class Builtin(val name: String) : ObtentionMethod()
        class ByIsolatingVariable(val variable: String, val from: Formula): ObtentionMethod()
        class ByCombiningFormulas(val formulas: List<Formula>): ObtentionMethod()
    }

    override fun translateToAppropriateFormInOrderToCompute(fieldName: String, owner: Component, system: PhysicalSystem): Formula {
        val variable = findVariableCorrespondingTo(fieldName, owner)
        return (if (variable == outputVariable) this else isolateVariable(variable)).let {
            if (Settings.fieldComputationMethod == FieldComputationMethod.LAZY) it
            else it.composeWithNestedFormulasOf(owner, system)
        }
    }

    private fun isolateVariable(variable: String): Formula {
        if (variable == outputVariable) return this

        val owningRequirement = requirements.single { variable in it.ownedVariables }
        val refactoredRequirements = refactorRequirementsToIsolateVariable(variable)
        val modifiedExpression = expression.isolateVariable(variable)

        return if (refactoredRequirements == requirements) this else Formula(
            ObtentionMethod.ByIsolatingVariable(variable, from = this),
            implicit = implicit,
            refactoredRequirements,
            variable to owningRequirement.alias + "." + owningRequirement.ownedVariables.getValue(variable),
            modifiedExpression
        )
    }

    private fun composeWithNestedFormulasOf(outputOwner: Component, system: PhysicalSystem): Formula {
        val requiredComponents = selectRequiredComponentsIn(system, outputOwner)
        val requiredFields = requirements.map { it.fetchRequiredFieldsIn(requiredComponents) }.reduce { acc, map -> acc.mergedWith(map, merge = { k, _, _ -> throw VariableNameCrashError(k) }) }
        var result: Formula = this

        for ((variable, field) in requiredFields) {
            if (field.obtainedBy is Formula && !(field.obtainedBy as Formula).implicit) {
                result = result.plug(field.obtainedBy as Formula, where = variable)
            }
        }

        return result
    }

    private fun plug(formula: Formula, where: String): Formula = plugImpl(formula, variable = where)
    private fun plugImpl(formula: Formula, variable: String): Formula {
        val (oldAliasesOfThatFormulaLinkedToNewOnes, aliasesLinkedToTheirNumericalSuffixes) = updateAliasesToAvoidNameCrashes(this, formula)
        val (oldAliasesOfPluggedFormulaLinkedToNewOnes, _) = updateAliasesToAvoidNameCrashes(formula, this, aliasesLinkedToTheirNumericalSuffixes)
        val renamedRequirementsOfThatFormula = requirements.map { it.withAliasesReferencesUpdated(oldAliasesOfThatFormulaLinkedToNewOnes) }
        val renamedRequirementsOfPluggedFormula = formula.requirements.map { it.withAliasesReferencesUpdated(oldAliasesOfPluggedFormulaLinkedToNewOnes) }

        val requirementsToBeFusedTogether = mutableListOf(renamedRequirementsOfThatFormula.single { variable in it.ownedVariables } to renamedRequirementsOfPluggedFormula.single { it.alias == oldAliasesOfPluggedFormulaLinkedToNewOnes[formula.outputOwnerAlias] } )
        while (true) {
            val (requirementOnThisSide, requirementOnPluggedSide) = requirementsToBeFusedTogether.last()
            if (requirementOnThisSide.isLocatedAtRoot || requirementOnPluggedSide.isLocatedAtRoot) break
            requirementsToBeFusedTogether.add(ownerOf(requirementOnThisSide, renamedRequirementsOfThatFormula) to ownerOf(requirementOnPluggedSide, renamedRequirementsOfPluggedFormula))
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
            requirementsToBeFusedTogether.map { (r1, r2) -> r1.fuseWith(r2) }
        )

        val combinedFormulas = (if (this.obtentionMethod is ObtentionMethod.ByCombiningFormulas) this.obtentionMethod.formulas else listOf(this)) +
                if (formula.obtentionMethod is ObtentionMethod.ByCombiningFormulas) formula.obtentionMethod.formulas else listOf(formula)

        return Formula(
            obtentionMethod = ObtentionMethod.ByCombiningFormulas(combinedFormulas),
            implicit = this.implicit && formula.implicit,
            requirements = newRequirements,
            output = outputVariable to "${oldAliasesOfThatFormulaLinkedToNewOnes[outputOwnerAlias]}.$outputField",
            expression = expression.composeWith(formula.expression, variable),
        )
    }

    private fun ownerOf(requirement: ComponentRequirement, where: List<ComponentRequirement>) =
        where.single { it.alias == requirement.ownerAlias }

    private fun updateAliasesToAvoidNameCrashes(
        formula: Formula,
        conflictingFormula: Formula,
        _aliasesLinkedToTheirNumericalSuffixes: Map<String, Int> = emptyMap()
    ) : Pair<Map<String, String>, Map<String, Int>> {

        fun String.suppressNumericalSuffix(): String {
            return if (this.last().isDigit() && this.dropLastWhile { it.isDigit() }.endsWith("-")) {
                this.dropLastWhile { it.isDigit() }.removeSuffix("-")
            } else this
        }

        val oldAliasesLinkedToNewOnesInThatFormula = formula.requirements.map { it.alias }.associateWithTo(mutableMapOf()) { it }
        val aliasesInUseInOtherFormula = conflictingFormula.requirements.map { it.alias.suppressNumericalSuffix() }
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

    override fun compute(field: Field<*>, system: PhysicalSystem): PhysicalDouble {
        val outputOwner = system.findFieldOwner(field)
        try {
            val arguments = fetchVariablesValuesIn(system, outputOwner)
            return expression.compute(arguments.mapValues { (_, v) -> v.toPhysicalDouble() })
        } catch (e: KnowledgeException) {
            throw InappropriateKnowledgeException(this, "${outputOwner.name}(...).${field.name}", e.message)
        }
    }

    override fun toString(): String {
        return expression.toFlatString()
    }
}
