package beer.thierry.budgetplannerrest.service.email

import beer.thierry.budgetplannerrest.model.user.User

interface IRegisterEmailService {
    fun sendRegistrationEmail(user: User, token: String)
}
