package beer.thierry.budgetplannerrest.service.auth

import beer.thierry.budgetplannerrest.model.auth.AuthRegisterRequest
import beer.thierry.budgetplannerrest.model.auth.AuthRequest
import beer.thierry.budgetplannerrest.model.auth.AuthResponse

interface IAuthService {
    fun authenticate(authRequest: AuthRequest): AuthResponse
    fun register(authRequest: AuthRegisterRequest): AuthResponse
    fun confirmRegistration(token: String, username: String): Boolean
}
