package dto

import kotlin.math.min

class Token(
    val value: String,
    val type: String,
    val start: Int
) {
    val length = value.length
    val span = start..start+length
    val end = span.last

    fun concatenateWith(other: Token, newType: String) = Token(
        value + other.value,
        newType,
        min(start, other.start)
    )
}
