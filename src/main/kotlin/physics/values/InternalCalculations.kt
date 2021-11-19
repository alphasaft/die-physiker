package physics.values

import kotlin.math.abs
import remove


internal fun Double.suppressDwindlingDigits(): Double {
    var asString = toString()

    if (asString.split(".").last().length < 10) return this

    val splitOnE = asString.split("E")
    if (splitOnE.size > 1) {
        val (coefficient, exponent) = splitOnE.first() to splitOnE.last().toInt()
        asString = if (exponent < 0) {
            ("0." + "0".repeat(-exponent - 1) + coefficient.remove(".")).removeSuffix("0")
        } else {
            coefficient.remove(".") + "0".repeat(exponent-1)
        }
    }

    return if (asString.last().digitToInt() < 5) asString.dropLast(1).dropLastWhile { it in ".0" }.toDouble()
    else asString.dropLast(1).dropLastWhile { it in ".9" }.let { it.dropLast(1) + (it.last().digitToInt()+1) }.toDouble()
}

internal fun Boolean.toInt() = if (this) 1 else 0


private fun isFramedBetween1And10(value: Double) = value >= 1.0 && value < 10.0

internal fun Double.scientificNotation(): Pair<Double, Int> {
    var coefficient = this
    var exponent = 0
    val action = when {
        this == 0.0 -> return 0.0 to 0
        abs(this) < 1.0 -> fun() { coefficient *= 10; exponent -= 1 }
        abs(this) >= 10.0 -> fun() { coefficient /= 10; exponent += 1 }
        else -> return coefficient to exponent
    }
    while (!isFramedBetween1And10(abs(coefficient))) action()
    return coefficient.suppressDwindlingDigits() to exponent
}

internal val String.significantDigitsCount get() = remove("-").remove(".").dropWhile { it == '0' }.length
