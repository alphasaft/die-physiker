package nlp

typealias WordInstanceList = List<WordInstance>

fun WordInstanceList.removeUnknown(): WordInstanceList = this.filterNot { it.model.name == "<unknown>" }
val WordInstanceList.values get() = this.map { it.value }
