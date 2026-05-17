package beer.thierry.centsible.api.services.email

import beer.thierry.centsible.api.model.user.User
import java.util.Locale

interface IRegisterEmailService {
    /**
     * Sends the post-registration confirmation email. [locale] selects which translation
     * bundle ("en", "fr", "de") populates the subject and body; unknown values fall back
     * to English. When asynchronous email sending is added later, the locale must travel
     * with the job because `LocaleContextHolder` is request-scoped.
     */
    fun sendRegistrationEmail(user: User, token: String, locale: Locale)
}
