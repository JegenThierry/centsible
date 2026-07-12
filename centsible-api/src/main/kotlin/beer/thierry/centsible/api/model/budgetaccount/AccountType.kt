package beer.thierry.centsible.api.model.budgetaccount

/**
 * Classification of a budget account. Persisted as the account's `type` column (VARCHAR, mirrored by
 * the `accounts_type_check` constraint). The type records the classification only; every account's
 * balance is taken at face value (positive unless the stored balance is itself negative), so no type
 * has its sign flipped when totalling balances or net worth.
 */
enum class AccountType {
    CHECKING, SAVINGS, CASH, CREDIT_CARD, INVESTMENT, ASSET, LOAN, MORTGAGE, OTHER;

    companion object {
        fun fromValue(value: String): AccountType =
            runCatching { valueOf(value.uppercase()) }.getOrElse { CHECKING }
    }
}
