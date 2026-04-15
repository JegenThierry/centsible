package beer.thierry.budgetplanner.api.service.email

import beer.thierry.budgetplanner.api.model.user.User

interface IRegisterEmailService {
    fun sendRegistrationEmail(user: User, token: String)
}
