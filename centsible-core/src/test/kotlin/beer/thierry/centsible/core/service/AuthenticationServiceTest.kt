package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.auth.AuthRequest
import beer.thierry.centsible.api.model.auth.LoginResult
import beer.thierry.centsible.api.model.user.User
import beer.thierry.centsible.api.repository.ITwoFactorRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.authentication.ITotpService
import beer.thierry.centsible.api.services.email.IPasswordResetEmailService
import beer.thierry.centsible.api.services.email.IRegisterEmailService
import beer.thierry.centsible.core.services.authentication.AuthenticationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.OffsetDateTime
import java.util.Locale
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AuthenticationServiceTest {

    @Mock private lateinit var userRepository: IUserRepository
    @Mock private lateinit var twoFactorRepository: ITwoFactorRepository
    @Mock private lateinit var totpService: ITotpService
    @Mock private lateinit var passwordEncoder: PasswordEncoder
    @Mock private lateinit var registerEmailService: IRegisterEmailService
    @Mock private lateinit var passwordResetEmailService: IPasswordResetEmailService

    private val validJwtSecret = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    private fun service() = AuthenticationService(
        userRepository = userRepository,
        twoFactorRepository = twoFactorRepository,
        totpService = totpService,
        passwordEncoder = passwordEncoder,
        registerEmailService = registerEmailService,
        passwordResetEmailService = passwordResetEmailService,
        skipEmailVerification = false,
        jwtSecret = validJwtSecret,
        jwtExpirationMs = 3_600_000L,
        registrationEnabled = false,
    )

    private fun user(registered: Boolean, totpEnabled: Boolean = false) = User(
        username = "alice",
        email = "alice@example.com",
        passwordHash = "\$2a\$12\$storedhashstoredhashstoredhashstoredhashstor",
        registered = registered,
        totpEnabled = totpEnabled,
    )

    @Test
    fun `unknown username still runs a bcrypt verify so timing matches a real user (no enumeration oracle)`() {
        `when`(userRepository.findUserByUsername("ghost")).thenReturn(null)
        `when`(passwordEncoder.encode(anyString())).thenReturn("\$2a\$12\$dummydummydummydummydummydummydummydummydu")

        val ex = assertThrows(LocalizedException.Unauthorized::class.java) {
            service().authenticate(AuthRequest("ghost", "whatever"))
        }
        assertEquals("error.auth.invalidCredentials", ex.messageKey)
        verify(passwordEncoder).matches(eq("whatever"), anyString())
    }

    @Test
    fun `wrong password returns the same generic message as an unknown username`() {
        `when`(userRepository.findUserByUsername("alice")).thenReturn(user(registered = true))
        `when`(passwordEncoder.matches(eq("bad"), anyString())).thenReturn(false)

        val ex = assertThrows(LocalizedException.Unauthorized::class.java) {
            service().authenticate(AuthRequest("alice", "bad"))
        }
        assertEquals("error.auth.invalidCredentials", ex.messageKey)
    }

    @Test
    fun `unconfirmed account is only revealed after a correct password (not a pre-auth oracle)`() {
        `when`(userRepository.findUserByUsername("alice")).thenReturn(user(registered = false))
        `when`(passwordEncoder.matches(eq("correct"), anyString())).thenReturn(true)

        val ex = assertThrows(LocalizedException.Unauthorized::class.java) {
            service().authenticate(AuthRequest("alice", "correct"))
        }
        assertEquals("error.auth.emailNotConfirmed", ex.messageKey)
    }

    @Test
    fun `valid credentials for a confirmed account without 2FA return a token`() {
        `when`(userRepository.findUserByUsername("alice")).thenReturn(user(registered = true))
        `when`(passwordEncoder.matches(eq("correct"), anyString())).thenReturn(true)

        val result = service().authenticate(AuthRequest("alice", "correct"))
        val authenticated = assertInstanceOf(LoginResult.Authenticated::class.java, result)
        assertTrue(authenticated.token.isNotBlank())
        verifyNoInteractions(twoFactorRepository)
    }

    @Test
    fun `valid credentials for a 2FA account issue a pre-auth token instead of a JWT`() {
        val twoFa = user(registered = true, totpEnabled = true)
        `when`(userRepository.findUserByUsername("alice")).thenReturn(twoFa)
        `when`(passwordEncoder.matches(eq("correct"), anyString())).thenReturn(true)

        val result = service().authenticate(AuthRequest("alice", "correct"))
        val pending = assertInstanceOf(LoginResult.TwoFactorRequired::class.java, result)
        assertTrue(pending.pendingToken.isNotBlank())
        verify(twoFactorRepository).deletePendingAuthForUser(twoFa.id)
        verify(twoFactorRepository).createPendingAuth(
            anyArg(UUID::class.java, twoFa.id),
            anyArg(ByteArray::class.java, ByteArray(0)),
            anyArg(OffsetDateTime::class.java, OffsetDateTime.now()),
        )
    }

    @Test
    fun `requestPasswordReset stays silent and does no work for an unknown username`() {
        `when`(userRepository.findUserByUsername("ghost")).thenReturn(null)

        service().requestPasswordReset("ghost")

        verify(userRepository).findUserByUsername("ghost")
        verifyNoMoreInteractions(userRepository)
        verifyNoInteractions(passwordResetEmailService)
    }

    @Test
    fun `requestPasswordReset persists a token and sends the email for a confirmed account`() {
        val confirmed = user(registered = true)
        `when`(userRepository.findUserByUsername("alice")).thenReturn(confirmed)
        `when`(
            userRepository.setPasswordResetToken(
                anyArg(UUID::class.java, confirmed.id),
                anyArg(ByteArray::class.java, ByteArray(0)),
                anyArg(OffsetDateTime::class.java, OffsetDateTime.now()),
            ),
        ).thenReturn(true)

        service().requestPasswordReset("alice")

        verify(passwordResetEmailService).sendPasswordResetEmail(
            anyArg(User::class.java, confirmed),
            anyString(),
            anyArg(Locale::class.java, Locale.ENGLISH),
        )
    }

    @Test
    fun `changePassword bumps the token version and returns a fresh token for the caller`() {
        val u = user(registered = true)
        `when`(userRepository.findUserById(u.id)).thenReturn(u)
        `when`(passwordEncoder.matches(eq("OldPass1!"), anyString())).thenReturn(true)
        `when`(passwordEncoder.matches(eq("NewPass1!"), anyString())).thenReturn(false)
        `when`(passwordEncoder.encode("NewPass1!")).thenReturn("newhash")
        `when`(userRepository.updatePassword(u.id, "newhash")).thenReturn(true)

        val token = service().changePassword(u.id, "OldPass1!", "NewPass1!")

        assertTrue(token.isNotBlank())
        verify(userRepository).updatePassword(u.id, "newhash")
        verify(userRepository).incrementTokenVersion(u.id)
    }

    @Test
    fun `signOutOtherSessions bumps the version and returns a fresh token`() {
        val u = user(registered = true)
        `when`(userRepository.incrementTokenVersion(u.id)).thenReturn(true)
        `when`(userRepository.findUserById(u.id)).thenReturn(u)

        val token = service().signOutOtherSessions(u.id)

        assertTrue(token.isNotBlank())
        verify(userRepository).incrementTokenVersion(u.id)
    }

    @Test
    fun `signOutOtherSessions throws when the user no longer exists`() {
        val id = UUID.randomUUID()
        `when`(userRepository.incrementTokenVersion(id)).thenReturn(false)

        assertThrows(LocalizedException.Unauthorized::class.java) {
            service().signOutOtherSessions(id)
        }
    }

    private fun <T : Any> anyArg(clazz: Class<T>, dummy: T): T {
        any(clazz)
        return dummy
    }
}
