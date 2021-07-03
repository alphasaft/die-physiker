package nlp

import nlp.words.UnknownWord
import kotlin.math.max
import dto.WordInstance


// Easier to understand
typealias Consumed = Int


// --- Resemblance analysis ---

private fun recover(what: String, reference: String, startIndex: Int, referenceIndex: Int): Int {
    var currentIndex = startIndex
    while (currentIndex < what.length && what[currentIndex] != reference[referenceIndex]) {
        currentIndex++
    }
    return currentIndex
}

fun String.resemblanceTo(other: String): Double {
    if (this == other) return 1.0

    var identicalLettersCount = 0
    var thisIndex = 0
    var otherIndex = 0

    while (thisIndex < this.length && otherIndex < other.length) {
        val indexOfCurrentThisCharInRemainingOtherInput = other.substring(otherIndex).indexOf(this[thisIndex])
        val indexOfCurrentOtherCharInRemainingThisInput = this.substring(thisIndex).indexOf(other[otherIndex])

        if (indexOfCurrentOtherCharInRemainingThisInput == -1 && indexOfCurrentThisCharInRemainingOtherInput == -1) {
            break
        } else if (indexOfCurrentOtherCharInRemainingThisInput == 0) {
            identicalLettersCount++
            thisIndex++
            otherIndex++
        } else if (indexOfCurrentOtherCharInRemainingThisInput == -1 || indexOfCurrentThisCharInRemainingOtherInput < indexOfCurrentOtherCharInRemainingThisInput) {
            thisIndex = recover(what = this, reference = other, startIndex = thisIndex, referenceIndex = otherIndex)
        } else if (indexOfCurrentThisCharInRemainingOtherInput == -1 || indexOfCurrentOtherCharInRemainingThisInput <= indexOfCurrentThisCharInRemainingOtherInput) {
            otherIndex = recover(what = other, reference = this, startIndex = otherIndex, referenceIndex = thisIndex)
        }
    }

    return identicalLettersCount.toDouble() / max(length, other.length)
}

private const val RESEMBLANCE_THRESHOLD = 0.7

infix fun String.resembles(other: String): Boolean {
    return this.resemblanceTo(other).overcomeResemblanceThreshold()
}

fun Double.overcomeResemblanceThreshold(): Boolean {
    return this >= RESEMBLANCE_THRESHOLD
}

// WordList
