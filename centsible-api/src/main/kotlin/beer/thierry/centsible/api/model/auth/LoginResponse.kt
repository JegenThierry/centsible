package beer.thierry.centsible.api.model.auth

/**
 * Body of the first login step. When [twoFactorRequired] is true the response carries HTTP 202 and
 * no token — the client must complete the TOTP challenge. Otherwise [token] holds the issued JWT
 * (also set as the auth cookie) and the status is 200.
 */
data class LoginResponse(
    val twoFactorRequired: Boolean = false,
    val token: String = "",
)
