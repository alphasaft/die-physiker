typealias Couple<T> = Pair<T, T>
typealias Predicate<T> = (T) -> Boolean
typealias Mapper<T> = (T) -> T
typealias Args<T> = Map<String, T>
typealias Collector<T> = (Args<T>) -> T
