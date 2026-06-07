package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.auth.AuthRegisterRequest
import beer.thierry.centsible.api.model.notification.NotificationSettingsDTO
import beer.thierry.centsible.api.model.user.User
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.jooq.generated.tables.references.USERS
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.JSONB
import org.jooq.impl.DSL.field
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.*

private val REGISTRATION_TOKEN_HASH = field("registration_token_hash", ByteArray::class.java)
private val REGISTRATION_TOKEN_EXPIRES_AT = field("registration_token_expires_at", OffsetDateTime::class.java)
private val PASSWORD_RESET_TOKEN_HASH = field("password_reset_token_hash", ByteArray::class.java)
private val PASSWORD_RESET_TOKEN_EXPIRES_AT = field("password_reset_token_expires_at", OffsetDateTime::class.java)
private val USER_FIELDS: Array<Field<*>> = USERS.fields()
private const val DEFAULT_LOCALE = "en"
private val SUPPORTED_LOCALES = setOf("en", "fr", "de")

private fun normaliseLocale(locale: String?): String =
    locale?.takeIf { it in SUPPORTED_LOCALES } ?: DEFAULT_LOCALE

private val NOTIFICATION_SETTINGS = field("notification_settings", JSONB::class.java)

private val log = LoggerFactory.getLogger(UserRepository::class.java)

@Repository
class UserRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) : IUserRepository {
    override fun findUserByUsername(username: String): User? {
        return dsl.select(*USER_FIELDS).from(USERS)
            .where(USERS.USERNAME.eq(username))
            .fetchOneInto(User::class.java)
    }

    override fun findUserByEmail(email: String): User? {
        return dsl.select(*USER_FIELDS).from(USERS)
            .where(USERS.EMAIL.eq(email))
            .fetchOneInto(User::class.java)
    }

    override fun findUserByEmailOrUsername(email: String, username: String): User? {
        return dsl.select(*USER_FIELDS).from(USERS)
            .where(USERS.EMAIL.eq(email).or(USERS.USERNAME.eq(username)))
            .fetchOneInto(User::class.java)
    }

    override fun findUserById(id: UUID): User? {
        return dsl.select(*USER_FIELDS).from(USERS)
            .where(USERS.ID.eq(id))
            .fetchOneInto(User::class.java)
    }

    override fun findUserByValidTokenHash(tokenHash: ByteArray): User? {
        return dsl.select(*USER_FIELDS).from(USERS)
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
            .set(USERS.LOCALE, normaliseLocale(user.locale))
            .set(field("registered", Boolean::class.java), false)
            .set(REGISTRATION_TOKEN_HASH, registrationTokenHash)
            .set(REGISTRATION_TOKEN_EXPIRES_AT, registrationTokenExpiresAt)
            .set(USERS.CREATED_AT, OffsetDateTime.now())
            .set(USERS.MODIFIED_AT, OffsetDateTime.now())
            .returningResult(*USER_FIELDS)
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
            .returningResult(*USER_FIELDS)
            .fetchOneInto(User::class.java)
    }

    override fun updateUserLocale(id: UUID, locale: String): User? {
        return dsl.update(USERS)
            .set(USERS.LOCALE, normaliseLocale(locale))
            .set(USERS.MODIFIED_AT, OffsetDateTime.now())
            .where(USERS.ID.eq(id))
            .returningResult(*USER_FIELDS)
            .fetchOneInto(User::class.java)
    }

    override fun setPasswordResetToken(id: UUID, tokenHash: ByteArray, expiresAt: OffsetDateTime): Boolean {
        return dsl.update(USERS)
            .set(PASSWORD_RESET_TOKEN_HASH, tokenHash)
            .set(PASSWORD_RESET_TOKEN_EXPIRES_AT, expiresAt)
            .set(USERS.MODIFIED_AT, OffsetDateTime.now())
            .where(USERS.ID.eq(id))
            .execute() > 0
    }

    override fun findUserByValidPasswordResetTokenHash(tokenHash: ByteArray): User? {
        return dsl.select(*USER_FIELDS).from(USERS)
            .where(PASSWORD_RESET_TOKEN_HASH.eq(tokenHash))
            .and(PASSWORD_RESET_TOKEN_EXPIRES_AT.gt(OffsetDateTime.now()))
            .fetchOneInto(User::class.java)
    }

    override fun resetPassword(id: UUID, newPasswordHash: String): Boolean {
        return dsl.update(USERS)
            .set(USERS.PASSWORD_HASH, newPasswordHash)
            .set(PASSWORD_RESET_TOKEN_HASH, null as ByteArray?)
            .set(PASSWORD_RESET_TOKEN_EXPIRES_AT, null as OffsetDateTime?)
            .set(USERS.MODIFIED_AT, OffsetDateTime.now())
            .where(USERS.ID.eq(id))
            .execute() > 0
    }

    override fun fetchAllUserIds(): List<UUID> =
        dsl.select(USERS.ID).from(USERS).fetch { it[USERS.ID]!! }

    override fun fetchNotificationSettings(id: UUID): NotificationSettingsDTO {
        val jsonb = dsl.select(NOTIFICATION_SETTINGS)
            .from(USERS)
            .where(USERS.ID.eq(id))
            .fetchOne(NOTIFICATION_SETTINGS)
        val raw = jsonb?.data() ?: return NotificationSettingsDTO()
        return runCatching {
            objectMapper.readValue(raw, NotificationSettingsDTO::class.java)
        }.getOrElse { ex ->
            log.warn("Failed to parse notification_settings for user {}, falling back to defaults", id, ex)
            NotificationSettingsDTO()
        }
    }

    override fun updateNotificationSettings(
        id: UUID,
        settings: NotificationSettingsDTO,
    ): NotificationSettingsDTO {
        val jsonValue = JSONB.valueOf(objectMapper.writeValueAsString(settings))
        dsl.update(USERS)
            .set(NOTIFICATION_SETTINGS, jsonValue)
            .set(USERS.MODIFIED_AT, OffsetDateTime.now())
            .where(USERS.ID.eq(id))
            .execute()
        return settings
    }

    override fun savePendingTotpSecret(id: UUID, encryptedSecret: ByteArray): Boolean =
        dsl.update(USERS)
            .set(USERS.TOTP_PENDING_SECRET_ENCRYPTED, encryptedSecret)
            .set(USERS.MODIFIED_AT, OffsetDateTime.now())
            .where(USERS.ID.eq(id))
            .execute() > 0

    override fun getPendingTotpSecret(id: UUID): ByteArray? =
        dsl.select(USERS.TOTP_PENDING_SECRET_ENCRYPTED).from(USERS)
            .where(USERS.ID.eq(id))
            .fetchOne(USERS.TOTP_PENDING_SECRET_ENCRYPTED)

    override fun getActiveTotpSecret(id: UUID): ByteArray? =
        dsl.select(USERS.TOTP_SECRET_ENCRYPTED).from(USERS)
            .where(USERS.ID.eq(id).and(USERS.TOTP_ENABLED.isTrue))
            .fetchOne(USERS.TOTP_SECRET_ENCRYPTED)

    override fun activateTotp(id: UUID, encryptedSecret: ByteArray): Boolean =
        dsl.update(USERS)
            .set(USERS.TOTP_SECRET_ENCRYPTED, encryptedSecret)
            .set(USERS.TOTP_PENDING_SECRET_ENCRYPTED, null as ByteArray?)
            .set(USERS.TOTP_ENABLED, true)
            .set(USERS.TOTP_LAST_USED_STEP, null as Long?)
            .set(USERS.TOTP_ENABLED_AT, OffsetDateTime.now())
            .set(USERS.MODIFIED_AT, OffsetDateTime.now())
            .where(USERS.ID.eq(id))
            .execute() > 0

    override fun disableTotp(id: UUID): Boolean =
        dsl.update(USERS)
            .set(USERS.TOTP_SECRET_ENCRYPTED, null as ByteArray?)
            .set(USERS.TOTP_PENDING_SECRET_ENCRYPTED, null as ByteArray?)
            .set(USERS.TOTP_ENABLED, false)
            .set(USERS.TOTP_LAST_USED_STEP, null as Long?)
            .set(USERS.TOTP_ENABLED_AT, null as OffsetDateTime?)
            .set(USERS.MODIFIED_AT, OffsetDateTime.now())
            .where(USERS.ID.eq(id))
            .execute() > 0

    override fun getTotpLastUsedStep(id: UUID): Long? =
        dsl.select(USERS.TOTP_LAST_USED_STEP).from(USERS)
            .where(USERS.ID.eq(id))
            .fetchOne(USERS.TOTP_LAST_USED_STEP)

    override fun updateTotpLastUsedStep(id: UUID, step: Long): Boolean =
        dsl.update(USERS)
            .set(USERS.TOTP_LAST_USED_STEP, step)
            .where(USERS.ID.eq(id))
            .execute() > 0
}
