package beer.thierry.budgetplanner.jooq.repository

import beer.thierry.budgetplanner.api.model.auth.AuthRegisterRequest
import beer.thierry.budgetplanner.api.model.user.User
import beer.thierry.budgetplanner.api.repository.IUserRepository
import beer.thierry.jooq.generated.tables.references.USERS
import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.*

private val REGISTRATION_TOKEN_HASH = field("registration_token_hash", ByteArray::class.java)
private val REGISTRATION_TOKEN_EXPIRES_AT = field("registration_token_expires_at", OffsetDateTime::class.java)

@Repository
class UserRepository(private val dsl: DSLContext) : IUserRepository {
    override fun findUserByUsername(username: String): User? {
        return dsl.selectFrom(USERS)
            .where(USERS.USERNAME.eq(username))
            .fetchOneInto(User::class.java)
    }

    override fun findUserByEmail(email: String): User? {
        return dsl.selectFrom(USERS)
            .where(USERS.EMAIL.eq(email))
            .fetchOneInto(User::class.java)
    }

    override fun findUserByEmailOrUsername(email: String, username: String): User? {
        return dsl.selectFrom(USERS)
            .where(USERS.EMAIL.eq(email).or(USERS.USERNAME.eq(username)))
            .fetchOneInto(User::class.java)
    }

    override fun findUserById(id: UUID): User? {
        return dsl.selectFrom(USERS)
            .where(USERS.ID.eq(id))
            .fetchOneInto(User::class.java)
    }

    override fun findUserByValidTokenHash(tokenHash: ByteArray): User? {
        return dsl.selectFrom(USERS)
            .where(REGISTRATION_TOKEN_HASH.eq(tokenHash))
            .and(REGISTRATION_TOKEN_EXPIRES_AT.gt(OffsetDateTime.now()))
            .fetchOneInto(User::class.java)
    }

    override fun createUser(
        user: AuthRegisterRequest,
        passwordHash: String,
        registrationTokenHash: ByteArray,
        registrationTokenExpiresAt: OffsetDateTime,
    ): User? {
        return dsl.insertInto(USERS)
            .set(USERS.USERNAME, user.username)
            .set(USERS.EMAIL, user.email)
            .set(USERS.FIRST_NAME, user.firstName)
            .set(USERS.LAST_NAME, user.lastName)
            .set(USERS.PASSWORD_HASH, passwordHash)
            .set(field("registered", Boolean::class.java), false)
            .set(REGISTRATION_TOKEN_HASH, registrationTokenHash)
            .set(REGISTRATION_TOKEN_EXPIRES_AT, registrationTokenExpiresAt)
            .set(USERS.CREATED_AT, OffsetDateTime.now())
            .set(USERS.MODIFIED_AT, OffsetDateTime.now())
            .returning()
            .fetchOneInto(User::class.java)
    }

    override fun confirmUser(id: UUID): Boolean {
        return dsl.update(USERS)
            .set(field("registered", Boolean::class.java), true)
            .set(REGISTRATION_TOKEN_HASH, null as ByteArray?)
            .set(REGISTRATION_TOKEN_EXPIRES_AT, null as OffsetDateTime?)
            .set(USERS.MODIFIED_AT, OffsetDateTime.now())
            .where(USERS.ID.eq(id))
            .execute() > 0
    }

    override fun updateUserProfile(
        id: UUID,
        firstName: String,
        lastName: String,
        email: String,
        profilePicture: String?
    ): User? {
        return dsl.update(USERS)
            .set(USERS.FIRST_NAME, firstName)
            .set(USERS.LAST_NAME, lastName)
            .set(USERS.EMAIL, email)
            .set(USERS.PROFILE_PICTURE, profilePicture)
            .set(USERS.MODIFIED_AT, OffsetDateTime.now())
            .where(USERS.ID.eq(id))
            .returning()
            .fetchOneInto(User::class.java)
    }
}
