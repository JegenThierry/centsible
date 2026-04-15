package beer.thierry.budgetplanner.api.model.auth

data class AuthRequest(
    var username: String = "",
    var password: String = "",
)