package beer.thierry.centsible.api.model.budgetaccount

/**
 * Classification of a budget account. Persisted as the account's `type` column (VARCHAR, mirrored by
 * the `accounts_type_check` constraint). Liability-style types (CREDIT_CARD, LOAN, MORTGAGE) are the
 * foundation for future true-net-worth signing; v1 only records the classification.
 */
enum class AccountType {
    CHECKING, SAVINGS, CASH, CREDIT_CARD, INVESTMENT, ASSET, LOAN, MORTGAGE, OTHER;

    /** Liability-style accounts subtract from net worth — their balance magnitude is debt, not wealth. */
    val isLiability: Boolean
        get() = this == CREDIT_CARD || this == LOAN || this == MORTGAGE

    companion object {
        fun fromValue(value: String): AccountType =
            runCatching { valueOf(value.uppercase()) }.getOrElse { CHECKING }
    }
}
