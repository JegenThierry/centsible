package beer.thierry.centsible.jooq.repository

import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import org.jooq.DSLContext
import java.util.UUID

internal object ManagedCategoryNames {
    const val LENDING = "Lending"
    const val REPAYMENT = "Repayment"
    const val TRANSFER_OUT = "Transfer out"
    const val TRANSFER_IN = "Transfer in"
}

internal fun DSLContext.ensureAccountOwnedByUser(accountId: UUID, userId: UUID) {
    val exists = fetchExists(
        selectOne().from(ACCOUNTS).where(ACCOUNTS.ID.eq(accountId).and(ACCOUNTS.USER_ID.eq(userId)))
    )
    if (!exists) throw IllegalArgumentException("Account not found.")
}

internal fun DSLContext.findManagedCategoryId(name: String): Long? =
    select(CATEGORIES.ID)
        .from(CATEGORIES)
        .where(CATEGORIES.NAME.eq(name).and(CATEGORIES.IS_MANAGED.isTrue))
        .fetchOne()
        ?.get(CATEGORIES.ID)
