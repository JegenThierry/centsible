package beer.thierry.budgetplanner.api.service.auth

import beer.thierry.budgetplanner.api.model.auth.AuthRegisterRequest
import beer.thierry.budgetplanner.api.model.auth.AuthRequest
import beer.thierry.budgetplanner.api.model.auth.AuthResponse

interface IAuthService {
    fun authenticate(authRequest: AuthRequest): AuthResponse
    fun register(authRequest: AuthRegisterRequest): AuthResponse
    fun confirmRegistration(token: String, username: String): Boolean
}
