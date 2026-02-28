package beer.thierry.budgetplannerrest.repository.users

import beer.thierry.budgetplannerrest.model.auth.AuthRegisterRequest
import beer.thierry.budgetplannerrest.model.user.User
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.jooq.generated.tables.references.USERS
import org.jooq.DSLContext
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class UserRepository(private val dsl: DSLContext) : IUserRepository {
    override fun findAllUsers(): List<UserDTO> {
        return dsl.select(USERS.ID, USERS.USERNAME, USERS.EMAIL)
            .from(USERS)
            .fetchInto(UserDTO::class.java)
    }

    override fun findUserByUsername(username: String): User? {
        return dsl.selectFrom(USERS)
            .where(USERS.USERNAME.eq(username))
            .fetchOneInto(User::class.java)
    }

    override fun findUserById(id: UUID): User? {
        return dsl.selectFrom(USERS)
            .where(USERS.ID.eq(id))
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
            .set(USERS.CREATED_AT, OffsetDateTime.now())
            .set(USERS.MODIFIED_AT, OffsetDateTime.now())
            .returning()
            .fetchOneInto(User::class.java)
    }
}