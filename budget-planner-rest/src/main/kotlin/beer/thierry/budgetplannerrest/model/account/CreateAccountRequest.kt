package beer.thierry.budgetplannerrest.model.account

data class CreateAccountRequest(
    val name: String,
    val initialBalance: Number,
    val currency: Currency? = Currency.EUR,
)