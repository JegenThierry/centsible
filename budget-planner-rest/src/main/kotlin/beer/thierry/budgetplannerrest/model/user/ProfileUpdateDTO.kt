package beer.thierry.budgetplannerrest.model.user

data class ProfileUpdateDTO(
    val firstName: String,
    val lastName: String,
    val email: String
)
