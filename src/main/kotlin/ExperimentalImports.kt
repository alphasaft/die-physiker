@file:OptIn(ExperimentalStdlibApi::class, ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference
import kotlin.collections.buildList as nativeBuildList


fun <T> buildList(@BuilderInference initializer: MutableList<T>.() -> Unit): List<T> {
    return nativeBuildList(initializer)
}

inline fun <reified T> buildArray(@BuilderInference initializer: MutableList<T>.() -> Unit): Array<T> {
    return nativeBuildList(initializer).toTypedArray()
}

