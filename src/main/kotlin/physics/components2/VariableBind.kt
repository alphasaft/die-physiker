package physics.components2

import physics.components.Component
import physics.components.Location
import physics.quantities.PValue
import physics.quantities.Quantity
import physics.quantities.castAs
import kotlin.reflect.KClass


class VariableBind<T : PValue<T>>(
    private val variable: String,
    private val fieldLocation: Location.At,
    private val type: KClass<T>,
) {
    companion object InlineConstructor {
        inline operator fun <reified T : PValue<T>> invoke(
            variable: String,
            fieldLocation: Location.At
        ) = VariableBind(variable, fieldLocation, T::class)
    }

    fun bind(picks: Map<String, Component>, variables: MutableMap<String, Quantity<*>>) {
        if ("#" in variable) pluralBind(picks, variables)
        else simpleBind(picks, variables)
    }

    private fun pluralBind(picks: Map<String, Component>, variables: MutableMap<String, Quantity<*>>) {
        var i = 1
        while (fieldLocation.alias.replace("#", i.toString()) in picks) {
            variables[variable.replace("#", i.toString())] = picks
                .getValue(fieldLocation.alias.replace("#", i.toString()))
                .getQuantity(fieldLocation.field)
                .castAs(type)
            i++
        }
    }

    private fun simpleBind(picks: Map<String, Component>, variables: MutableMap<String, Quantity<*>>) {
        if (fieldLocation.alias !in picks) throw NoSuchElementException("No component registered under the alias ${fieldLocation.alias}")
        variables[variable] = picks.getValue(fieldLocation.alias).getQuantity(fieldLocation.field).castAs(type)
    }
}
