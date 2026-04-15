package beer.thierry.budgetplanner.api.model.user

data class ProfileUpdateDTO(
    var firstName: String = "",
    var lastName: String = "",
    var email: String = ""
)
