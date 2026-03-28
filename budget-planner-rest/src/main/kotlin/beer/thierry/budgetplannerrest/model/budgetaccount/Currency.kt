package beer.thierry.budgetplannerrest.model.budgetaccount

enum class Currency(val label: String) {
    EUR("EUR"),
    USD("USD"),
    YEN("YEN");

    override fun toString(): String = label
}