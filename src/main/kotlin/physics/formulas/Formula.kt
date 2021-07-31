package physics.formulas

import physics.FormulaException
import physics.InappropriateFormula
import physics.components.Component
import physics.components.Field
import physics.components.PhysicalSystem
import physics.values.*


// TODO : Faire lire la lettre et l'envoyer !!

class Formula(
    private val obtentionMethod: ObtentionMethod,
    override val requirements: List<Requirement>,
    variables: List<FormulaVariable>,
    val expression: Equality,
) : AbstractPhysicalRelationship() {
    constructor(
        name: String,
        vararg requirements: Requirement,
        variables: List<FormulaVariable>,
        expression: Equality
    ): this(ObtentionMethod.Builtin(name), requirements.toList(), variables, expression)

    override val inputVariables: List<FormulaVariable> = variables.filter { it.name != expression.variable }
    override val outputVariable = variables.single { it.name == expression.variable }

    sealed class ObtentionMethod {
        class Builtin(val name: String) : ObtentionMethod()
        class ByIsolatingVariable(val variable: String, val from: Formula): ObtentionMethod()
        class ByCombiningFormulas(val formulas: List<Formula>): ObtentionMethod()
    }

    override fun <T : PhysicalValue<*>> computeFieldValue(field: Field<T>, system: PhysicalSystem): Pair<T, Formula> {
        val fieldOwner = system.fetchFieldOwner(field)
        val appropriateForm = translateToAppropriateFormInOrderToCompute(field.name, fieldOwner, system)
        val result = appropriateForm.compute(field.name, fieldOwner, system)
        return result.castAs(field.type) to appropriateForm
    }

    fun compute(field: String, of: Component, system: PhysicalSystem): PhysicalDouble {
        val failureCause: FormulaException

        try {
            val correctFormula = translateToAppropriateFormInOrderToCompute(field, of, system)
            if (correctFormula !== this) return correctFormula.compute(field, of, system)

            val arguments = generateArgumentsFor(system, of)
            return expression.compute(arguments.mapValues { (_, v) -> v.toPhysicalDouble() })
        } catch (e: FormulaException) {
            failureCause = e
        }

        throw InappropriateFormula(this, "${of.name}(...).$field", failureCause.message)
    }

    private fun translateToAppropriateFormInOrderToCompute(field: String, of: Component, system: PhysicalSystem): Formula {
        return isolateVariable(field, of).composeWithNestedFormulasOf(system, outputOwner = of)
    }

    private fun isolateVariable(newOutputField: String, newOutputFieldOwner: Component): Formula {
        val (newOutputRequirement, modifiedRequirements) = refactorRequirementsToFitGivenOutput(newOutputField, newOutputFieldOwner)
        val variableToIsolate = inputVariables.first { it.represents(newOutputField, newOutputRequirement.name) }.name
        val modifiedExpression = expression.isolateVariable(variableToIsolate)

        return if (modifiedRequirements == requirements) this else Formula(
            ObtentionMethod.ByIsolatingVariable(variableToIsolate, from = this),
            modifiedRequirements,
            inputVariables + outputVariable,
            modifiedExpression
        )
    }

    private fun composeWithNestedFormulasOf(system: PhysicalSystem, outputOwner: Component): Formula {
        val selectedComponents = selectAppropriateComponentsIn(system, outputOwner)
        val fieldsCorrespondingToVariables = associateVariablesToFields(selectedComponents)
        var result: Formula = this

        for ((variable, field) in fieldsCorrespondingToVariables) {
            if (field.obtainedBy is Formula) {
                result = result.compose(variable, field.obtainedBy!! as Formula)
            }
        }

        return result
    }


    fun compose(variable: FormulaVariable, formula: Formula): Formula {
        val thisRequirements = requirements
        val otherRequirements = formula.requirements
        val usedNames = thisRequirements.map { it.name }

        fun unusedName(originalName: String): String {
            if (originalName !in usedNames) return originalName

            var i = 1
            while (originalName + i in usedNames) i++
            return originalName + i
        }

        val renamedRequirementsToAvoidNameCrashes = otherRequirements.map { it.withName(unusedName(it.name)) }
        val variablesRenamedAccordinglyToRequirements = formula.inputVariables.map { it.renameOwner(unusedName(it.name)) }

        return Formula(
            ObtentionMethod.ByCombiningFormulas(listOf(this, formula)),
            requirements - formula.requirementCorrespondingToOutputOwner + renamedRequirementsToAvoidNameCrashes,
            inputVariables - variable + variablesRenamedAccordinglyToRequirements + outputVariable,
            expression.composeWith(formula.expression)
        )
    }

    override fun toString(): String {
        return expression.toString()
    }
}







/*

Bonjour M. ...,

Je vous envoie ce mail pour vous faire part de mon envie, et, si le mot est approprié, de ma candidature
pour rejoindre [insérer nom du truc]. Ci-joint également mes bulletins de l'année de première puisqu'il me semble avoir
compris être nécessaire de les envoyer dans la foulée.

Dans un premier temps j'aimerais cependant m'excuser. En effet je n'ai pas vu ou entendu parler d'une date buttoir
pour vous faire parvenir ce message, mais cela ne veut pas dire qu'il n'y en avait pas - mon étourderie étant en passe
de devenir légendaire à plusieurs reprises auprès de mes proches -, et par conséquent j'espère sincèrement que mon
retard (si retard il y a) n'empêchera pas ceci d'atteindre son but.

Attaquons nous donc au vif du sujet, à savoir les raisons qui me poussent à vouloir me joindre à vous ces mercredis
après-midi. Les mathématiques et la physique ont, d'aussi longtemps que je me souvienne, toujours été des disciplines
qui m'ont beaucoup intéressé (la première encore plus que la seconde, mais là n'est pas le sujet, surtout que de
mémoire l'emploi du temps proposé s'accorde avec cette préférence personnelle), et auxquelles j'ai consacré un certain
temps notamment ces derniers mois et durant le confinement où j'ai commencé à lire un livre de mathématiques plutôt
épais - j'y suis d'ailleurs toujours, et il est rare que plusieurs jours se passent sans que je n'en lise quelques
pages -et ai farfouillé de temps à autre sur l'immense réserve de savoir qu'est Internet pour découvrir plus avant
quelques domaines de la physique qui m'intéressaient tout particulièrement. Outre cet intérêt sans but autre que celui
de satisfaire ma curiosité, je compte rejoindre les classes préparatoires de Faidherbe ; Ayant pour réputation de très
bien former les élèves, mais en contrepartie d'etre pour le moins corsées, il ne peut être que bénéfique pour moi de
m'avancer et découvrir la façon de travailler là-bas - qui selon beaucoup est assez différente de celle du lycée, que ce
soit au niveau du programme même ou de la façon dont les élèves sont "pris en charge" -  ne peut m'être que bénéfique.

Je pourrais sans doute encore avancer beaucoup d'arguments, comme le fait que j'espère pouvoir travailler dans l'un ou
l'autre de ces domaines plus tard, ou encore que mes amis me regardent d'un drôle d'air quand je leur dit que, je cite,
"les matrices c'est quand même vachement sympa", mais non seulement le but de ce mail n'est pas de vous endormir d'ennui
(mes félicitations si vous avez réussi à tenir jusqu'ici !), mais je crois également qu'au final cela ne ferait
qu'engloutir un vérité toute simple sous un flot d'anecdotes personnelles de plus en plus biscornues à mesure que les
manière de la dire s'épuise : j'aime sincèrement les mathématiques, et espère pouvoir vous rejoindre en face à face -
ou en videoconference si notre cher virus n'arrête pas de faire parler de lui, mais ne parlons pas des choses qui
fâchent - d'ici la rentrée.

Un grand merci, ne serait-ce que pour m'avoir prêté de votre temps,
Morel Hugo.

 */