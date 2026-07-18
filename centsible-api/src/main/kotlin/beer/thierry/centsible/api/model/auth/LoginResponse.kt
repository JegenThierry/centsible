package beer.thierry.centsible.api.model.auth

data class LoginResponse(
    val twoFactorRequired: Boolean = false,
    val token: String = "",
)
