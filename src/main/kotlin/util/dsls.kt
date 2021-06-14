package util


/** Annotates a function/method that should only be used for dsl purposes */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class DslFunction

/**
 * Annotates a method that is used as a dsl scope, that is that accept a single self-extending lambda
 * as its only argument, applies it to itself, and then returns `this`.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class DslScopeFunction


/**
 * Annotates a field that may be initialized in place, or by the use of a dsl function.
 * Dsl initializers of this field should be annotated with @[DslInitializer]
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
annotation class MayBeInitializedByDsl

/**
 * Indicates that this method is a prettified setter for the given [of] field.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class DslInitializer(val of: String)

/**
 * Annotates a field that may be mutated by the use of a dsl function, and is actually initialized
 * with a dummy value, unless provided. Dsl mutators of that field should be annotated with @[DslMutator].
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
annotation class MayBeMutatedByDsl

/**
 *  Indicates that this method is a prettified mutator (e.g a method that adds elements to a MutableList)
 *  for the given [of] field.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class DslMutator(val of: String)

