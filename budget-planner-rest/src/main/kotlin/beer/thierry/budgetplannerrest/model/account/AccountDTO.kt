package beer.thierry.budgetplannerrest.model.account

data class AccountDTO(
    val id: String,
    val name: String,
    val balance: Number,
    val currency: Currency,
)