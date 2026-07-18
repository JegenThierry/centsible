package beer.thierry.centsible.api.model.budgetaccount

enum class AccountType {
    CHECKING, SAVINGS, CASH, CREDIT_CARD, INVESTMENT, ASSET, LOAN, MORTGAGE, OTHER;

    companion object {
        fun fromValue(value: String): AccountType =
            runCatching { valueOf(value.uppercase()) }.getOrElse { CHECKING }
    }
}
