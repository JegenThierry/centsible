package beer.thierry.budgetplanner.api.services.email

import beer.thierry.budgetplanner.api.model.user.User
import java.util.Locale

interface IPasswordResetEmailService {
    /**
     * Sends the password reset email containing a one-time link. [locale] selects which
     * translation bundle ("en", "fr", "de") populates the subject and body; unknown values
     * fall back to English.
     */
    fun sendPasswordResetEmail(user: User, token: String, locale: Locale)
}
