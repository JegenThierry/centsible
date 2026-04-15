package beer.thierry.budgetplanner.api.model.auth

data class AuthRegisterRequest(
    var username: String = "",
    var password: String = "",
    var email: String = "",
    var firstName: String = "",
    var lastName: String = "",
)