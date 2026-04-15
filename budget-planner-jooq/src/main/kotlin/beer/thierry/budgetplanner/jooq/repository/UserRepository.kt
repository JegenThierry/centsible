package beer.thierry.budgetplanner.jooq.repository

import beer.thierry.budgetplanner.api.repository.IUserRepository
import beer.thierry.budgetplanner.api.model.auth.AuthRegisterRequest
import beer.thierry.budgetplanner.api.model.user.User
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.jooq.generated.tables.references.USERS
import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class UserRepository(private val dsl: DSLContext) : IUserRepository {
    override fun findAllUsers(): List<UserDTO> {
        return dsl.select(
            USERS.ID,
            USERS.USERNAME,
            USERS.EMAIL,
            USERS.FIRST_NAME,
            USERS.LAST_NAME,
            USERS.FIRST_NAME.concat(" ").concat(USERS.LAST_NAME).`as`("name"),
            USERS.PROFILE_PICTURE
        )
            .from(USERS)
            .fetchInto(UserDTO::class.java)
    }

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

    override fun findUserByTokenAndUsername(token: UUID, username: String): User? {
        return dsl.selectFrom(USERS)
            .where(field("registration_token", UUID::class.java).eq(token).and(USERS.USERNAME.eq(username)))
            .fetchOneInto(User::class.java)
    }

    override fun createUser(user: AuthRegisterRequest): User? {
        val encoder = BCryptPasswordEncoder()

        return dsl.insertInto(USERS)
            .set(USERS.USERNAME, user.username)
            .set(USERS.EMAIL, user.email)
            .set(USERS.FIRST_NAME, user.firstName)
            .set(USERS.LAST_NAME, user.lastName)
            .set(USERS.PASSWORD_HASH, encoder.encode(user.password))
            .set(field("registered", Boolean::class.java), false)
            .set(field("registration_token", UUID::class.java), UUID.randomUUID())
            .set(USERS.CREATED_AT, OffsetDateTime.now())
            .set(USERS.MODIFIED_AT, OffsetDateTime.now())
            .returning()
            .fetchOneInto(User::class.java)
    }

    override fun confirmUser(id: UUID): Boolean {
        return dsl.update(USERS)
            .set(field("registered", Boolean::class.java), true)
            .set(field("registration_token", UUID::class.java), null as UUID?)
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
