package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.admin.AdminUserDTO
import beer.thierry.centsible.api.repository.IAdminRepository
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import beer.thierry.jooq.generated.tables.references.USERS
import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.select
import org.jooq.impl.DSL.selectCount
import org.springframework.stereotype.Repository

@Repository
class AdminRepository(private val dsl: DSLContext) : IAdminRepository {

    override fun fetchAllUsers(): List<AdminUserDTO> {
        val accountCount = field(
            selectCount().from(ACCOUNTS).where(ACCOUNTS.USER_ID.eq(USERS.ID)),
        ).`as`("account_count")
        val transactionCount = field(
            selectCount()
                .from(TRANSACTIONS)
                .join(ACCOUNTS).on(TRANSACTIONS.ACCOUNT_ID.eq(ACCOUNTS.ID))
                .where(ACCOUNTS.USER_ID.eq(USERS.ID)),
        ).`as`("transaction_count")
        val lastTransactionAt = field(
            select(max(TRANSACTIONS.CREATED_AT))
                .from(TRANSACTIONS)
                .join(ACCOUNTS).on(TRANSACTIONS.ACCOUNT_ID.eq(ACCOUNTS.ID))
                .where(ACCOUNTS.USER_ID.eq(USERS.ID)),
        ).`as`("last_transaction_at")

        return dsl.select(
            USERS.ID,
            USERS.USERNAME,
            USERS.EMAIL,
            USERS.FIRST_NAME,
            USERS.LAST_NAME,
            USERS.REGISTERED,
            USERS.TOTP_ENABLED,
            USERS.CREATED_AT,
            USERS.LAST_LOGIN_AT,
            USERS.LAST_SEEN_AT,
            lastTransactionAt,
            accountCount,
            transactionCount,
        )
            .from(USERS)
            .orderBy(USERS.CREATED_AT.desc())
            .fetchInto(AdminUserDTO::class.java)
    }
}
