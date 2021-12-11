package loaders.base

/**
 * Purely informative annotation, used to explicitly precise that this method of the Parser already defines an ast
 * scope by its own, and doesn't need to be enclosed in another scope by the caller function.
 * If @DefinesScope isn't provided then the fact that no scope encloses this method is assumed.
 * It should be enforced by a warning soon.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class DefinesScope
