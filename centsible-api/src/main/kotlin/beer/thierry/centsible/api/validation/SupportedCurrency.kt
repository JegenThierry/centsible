package beer.thierry.centsible.api.validation

import beer.thierry.centsible.api.model.budgetaccount.Currency
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [SupportedCurrencyValidator::class])
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SupportedCurrency(
    val message: String = "{validation.currency.unsupported}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class SupportedCurrencyValidator : ConstraintValidator<SupportedCurrency, String?> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean =
        value == null || Currency.entries.any { it.name == value }
}
