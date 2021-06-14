import nlp.Corpus
import tokenizing.NLPBaseTokenizer
import tokenizing.removeWhitespaces
import nlp.words.*
import util.remove


fun main() {
    val tokenizer = NLPBaseTokenizer()
    val tokens = tokenizer.tokenize("sin(32.0) + 12,300").removeWhitespaces()

    val corpus = Corpus builtWith {
        - WordCategory("number").definedBy {
            - (case ("frNumber") thenConvertWith { it.remove(".").replace(",", ".").toDouble() })
            - (case ("enNumber") thenConvertWith { it.remove(",").toDouble() })
        }

        - WordCategory("operator")
        - WordCategory("lParenthesis")
        - WordCategory("rParenthesis")

        - (SimpleWord("funcName") matching listOf("sin", "cos", "tan") then (StrictWord("parenthesis") matching "(" )
                named "function"
                andConcatenateWith { words ->
                    val word = words[0]
                    listOf(WordInstance(this, name, word.value, word.start))
                })
    }

    println(tokens)
}



/*
 Une entreprise souhaite [vérifier] si la [norme] concernant une piscine,
 qui est que la [concentration en quantité de matière] de [chlore] [dans] la piscine (qu'on considère composée
 à 100%) soit [inférieure à] [2.0 mol/L] est [respectée].

 Il réalise donc une [dissolution] de [m = 33.3 g] de [chlore] dans [V = 1.0 L] d'[eau].

 Rappel : [M(Cl) = 30.0 g/mol]

 -> setLanguage(Language.FR)
 -> sanitaryStandard = Standard {
        return checkWithComparator(
            it.getMatterQuantityConcentration(),
            createComparatorFromNaturalLanguage("inférieure à", "2.0", "mol/L")
        )
    }

 -> dissolution = Dissolution.prepare {
        solvent = "eau", volume = Quantity(1, significantDigits = 2, unit = "L"))
        + Ingredient("dioxyde de carbone", volume = Quantity(33.3, significatDigits = 3, unit = "g")))
    }

 -> goal = Goals.Verify(sanitaryStandard)

 -> context = Context(
        sanitaryStandard,
        goal,
        dissolution,
    )

  -> print("Is it correct ?\n" + context.summarize())

 1) [Quelle] est la [quantité de matière] [dissoute] ?

 -> print(
        dissolution
            .solutes
            .checkSize(1)
            .getMatterQuantity()  // Default unit is  mol
    )

  2) /goal

  -> goal.solve()

 */

/*
The fact is that it's awfully complex for a computer to understand natural language.
But, because we're cheaters, we don't need to understand all of the text. Actually, just spotting
a few keywords - like 'solvent' or 'dissolution' - is enough.

Besides, the

 */
