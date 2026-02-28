package beer.thierry.budgetplannerrest.model.account

enum class Currency(val label: String) {
    EUR("EUR"),
    USD("USD"),
    YEN("YEN");

    override fun toString(): String = label
}