package beer.thierry.centsible.core.service.authentication

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.user.User
import beer.thierry.centsible.api.repository.ITwoFactorRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.core.services.authentication.TotpSecretCipher
import beer.thierry.centsible.core.services.authentication.TotpService
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.time.SystemTimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TotpServiceTest {

    // Raw ArgumentMatchers.eq()/any() return null, which trips Kotlin's not-null assertion on
    // non-null parameters — these helpers erase the platform type (same pattern as the sibling tests).
    private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()
    private fun <T> eqArg(value: T): T = org.mockito.ArgumentMatchers.eq(value) ?: value

    @Mock private lateinit var userRepository: IUserRepository
    @Mock private lateinit var twoFactorRepository: ITwoFactorRepository
    @Mock private lateinit var cipher: TotpSecretCipher
    @Mock private lateinit var passwordEncoder: PasswordEncoder

    private val testSecret = "TESTSECRETTESTSECRET1234"
    private val encryptedSecret = testSecret.toByteArray(Charsets.UTF_8)
    private val timeProvider = SystemTimeProvider()
    private val codeGenerator = DefaultCodeGenerator()

    private fun service() = TotpService(
        userRepository = userRepository,
        twoFactorRepository = twoFactorRepository,
        cipher = cipher,
        passwordEncoder = passwordEncoder,
    )

    private fun user(totpEnabled: Boolean = false) = User(
        username = "alice",
        email = "alice@example.com",
        passwordHash = "\$2a\$12\$storedhashstoredhashstoredhashstoredhashstor",
        registered = true,
        totpEnabled = totpEnabled,
    )

    @Test
    fun `status returns enabled false with zero remaining codes when 2FA is off`() {
        val u = user(totpEnabled = false)
        `when`(userRepository.findUserById(u.id)).thenReturn(u)

        val result = service().status(u.id)

        assertFalse(result.enabled)
        assertEquals(0, result.recoveryCodesRemaining)
    }

    @Test
    fun `status returns enabled true with the count of unused recovery codes`() {
        val u = user(totpEnabled = true)
        `when`(userRepository.findUserById(u.id)).thenReturn(u)
        `when`(twoFactorRepository.fetchUnusedRecoveryCodeHashes(u.id)).thenReturn(listOf("a", "b", "c"))

        val result = service().status(u.id)

        assertTrue(result.enabled)
        assertEquals(3, result.recoveryCodesRemaining)
    }

    @Test
    fun `status throws when user does not exist`() {
        val id = UUID.randomUUID()
        `when`(userRepository.findUserById(id)).thenReturn(null)

        assertThrows(LocalizedException.NotFound::class.java) {
            service().status(id)
        }
    }

    @Test
    fun `beginEnrollment stores encrypted secret and returns enrollment DTO`() {
        val u = user(totpEnabled = false)
        `when`(userRepository.findUserById(u.id)).thenReturn(u)
        `when`(cipher.encrypt(ArgumentMatchers.anyString())).thenReturn(encryptedSecret)
        `when`(userRepository.savePendingTotpSecret(eqArg(u.id), eqArg(encryptedSecret))).thenReturn(true)

        val result = service().beginEnrollment(u.id, u.email)

        assertNotNull(result.otpauthUri)
        assertTrue(result.otpauthUri.startsWith("otpauth://totp/"))
        assertTrue(result.secret.isNotBlank())
        verify(cipher).encrypt(result.secret)
    }

    @Test
    fun `beginEnrollment throws when 2FA is already enabled`() {
        val u = user(totpEnabled = true)
        `when`(userRepository.findUserById(u.id)).thenReturn(u)

        assertThrows(LocalizedException.Conflict::class.java) {
            service().beginEnrollment(u.id, u.email)
        }
    }

    @Test
    fun `beginEnrollment throws when user does not exist`() {
        val id = UUID.randomUUID()
        `when`(userRepository.findUserById(id)).thenReturn(null)

        assertThrows(LocalizedException.NotFound::class.java) {
            service().beginEnrollment(id, "alice@example.com")
        }
    }

    @Test
    fun `confirmEnrollment activates TOTP and returns recovery codes on valid code`() {
        val u = user(totpEnabled = false)
        `when`(userRepository.getPendingTotpSecret(u.id)).thenReturn(encryptedSecret)
        `when`(cipher.decrypt(encryptedSecret)).thenReturn(testSecret)
        `when`(cipher.encrypt(testSecret)).thenReturn(encryptedSecret)
        `when`(userRepository.activateTotp(eqArg(u.id), eqArg(encryptedSecret))).thenReturn(true)
        `when`(userRepository.updateTotpLastUsedStep(eqArg(u.id), ArgumentMatchers.anyLong())).thenReturn(true)
        `when`(passwordEncoder.encode(ArgumentMatchers.anyString())).thenReturn("\$2a\$12\$recoveryHash")

        val validCode = computeValidCode(testSecret)
        val result = service().confirmEnrollment(u.id, validCode)

        assertNotNull(result.recoveryCodes)
        assertEquals(16, result.recoveryCodes.size)
        verify(twoFactorRepository).replaceRecoveryCodes(eqArg(u.id), ArgumentMatchers.anyList())
        verify(userRepository).activateTotp(eqArg(u.id), eqArg(encryptedSecret))
    }

    @Test
    fun `confirmEnrollment throws when there is no pending secret`() {
        `when`(userRepository.getPendingTotpSecret(anyArg<UUID>())).thenReturn(null)

        assertThrows(LocalizedException.BadRequest::class.java) {
            service().confirmEnrollment(UUID.randomUUID(), "123456")
        }
    }

    @Test
    fun `confirmEnrollment throws on an invalid code`() {
        val u = user(totpEnabled = false)
        `when`(userRepository.getPendingTotpSecret(u.id)).thenReturn(encryptedSecret)
        `when`(cipher.decrypt(encryptedSecret)).thenReturn(testSecret)

        assertThrows(LocalizedException.BadRequest::class.java) {
            service().confirmEnrollment(u.id, "000000")
        }
    }

    @Test
    fun `disable deactivates TOTP and revokes recovery codes on a valid recovery code`() {
        val u = user(totpEnabled = true)
        val recoveryCode = "ABCD-EFGH-IJKL-MNOP"
        `when`(userRepository.findUserById(u.id)).thenReturn(u)
        `when`(userRepository.getActiveTotpSecret(u.id)).thenReturn(encryptedSecret)
        `when`(cipher.decrypt(encryptedSecret)).thenReturn(testSecret)
        `when`(twoFactorRepository.fetchUnusedRecoveryCodeHashes(u.id)).thenReturn(listOf("hash1"))
        `when`(passwordEncoder.matches(eqArg(recoveryCode), eqArg("hash1"))).thenReturn(true)
        `when`(twoFactorRepository.markRecoveryCodeUsed(u.id, "hash1")).thenReturn(true)
        `when`(userRepository.disableTotp(u.id)).thenReturn(true)
        `when`(twoFactorRepository.deleteRecoveryCodes(u.id)).thenReturn(1)

        service().disable(u.id, recoveryCode)

        verify(userRepository).disableTotp(u.id)
        verify(twoFactorRepository).deleteRecoveryCodes(u.id)
    }

    @Test
    fun `disable throws when 2FA is not enabled`() {
        val u = user(totpEnabled = false)
        `when`(userRepository.findUserById(u.id)).thenReturn(u)

        assertThrows(LocalizedException.BadRequest::class.java) {
            service().disable(u.id, "123456")
        }
    }

    @Test
    fun `disable throws on invalid code`() {
        val u = user(totpEnabled = true)
        `when`(userRepository.findUserById(u.id)).thenReturn(u)
        `when`(userRepository.getActiveTotpSecret(u.id)).thenReturn(encryptedSecret)
        `when`(cipher.decrypt(encryptedSecret)).thenReturn(testSecret)
        `when`(twoFactorRepository.fetchUnusedRecoveryCodeHashes(u.id)).thenReturn(emptyList())

        assertThrows(LocalizedException.BadRequest::class.java) {
            service().disable(u.id, "000000")
        }
    }

    @Test
    fun `cancelEnrollment clears the pending secret when one exists`() {
        val id = UUID.randomUUID()
        `when`(userRepository.clearPendingTotpSecret(id)).thenReturn(true)

        service().cancelEnrollment(id)

        verify(userRepository).clearPendingTotpSecret(id)
    }

    @Test
    fun `cancelEnrollment is a no-op when no pending secret exists`() {
        val id = UUID.randomUUID()
        `when`(userRepository.clearPendingTotpSecret(id)).thenReturn(false)

        service().cancelEnrollment(id)
    }

    @Test
    fun `regenerateRecoveryCodes replaces codes and returns new set on valid recovery code`() {
        val u = user(totpEnabled = true)
        val recoveryCode = "WXYZ-0123-4567-8901"
        `when`(userRepository.findUserById(u.id)).thenReturn(u)
        `when`(userRepository.getActiveTotpSecret(u.id)).thenReturn(encryptedSecret)
        `when`(cipher.decrypt(encryptedSecret)).thenReturn(testSecret)
        `when`(twoFactorRepository.fetchUnusedRecoveryCodeHashes(u.id)).thenReturn(listOf("hashA"))
        `when`(passwordEncoder.matches(eqArg(recoveryCode), eqArg("hashA"))).thenReturn(true)
        `when`(twoFactorRepository.markRecoveryCodeUsed(u.id, "hashA")).thenReturn(true)
        `when`(passwordEncoder.encode(ArgumentMatchers.anyString())).thenReturn("\$2a\$12\$newRecoveryHash")

        val result = service().regenerateRecoveryCodes(u.id, recoveryCode)

        assertEquals(16, result.recoveryCodes.size)
        verify(twoFactorRepository).replaceRecoveryCodes(eqArg(u.id), ArgumentMatchers.anyList())
    }

    @Test
    fun `regenerateRecoveryCodes throws when 2FA is not enabled`() {
        val u = user(totpEnabled = false)
        `when`(userRepository.findUserById(u.id)).thenReturn(u)

        assertThrows(LocalizedException.BadRequest::class.java) {
            service().regenerateRecoveryCodes(u.id, "123456")
        }
    }

    @Test
    fun `regenerateRecoveryCodes throws on invalid code`() {
        val u = user(totpEnabled = true)
        `when`(userRepository.findUserById(u.id)).thenReturn(u)
        `when`(userRepository.getActiveTotpSecret(u.id)).thenReturn(encryptedSecret)
        `when`(cipher.decrypt(encryptedSecret)).thenReturn(testSecret)
        `when`(twoFactorRepository.fetchUnusedRecoveryCodeHashes(u.id)).thenReturn(emptyList())

        assertThrows(LocalizedException.BadRequest::class.java) {
            service().regenerateRecoveryCodes(u.id, "000000")
        }
    }

    @Test
    fun `verifyChallengeCode returns true for a valid TOTP code`() {
        val u = user(totpEnabled = true)
        `when`(userRepository.getActiveTotpSecret(u.id)).thenReturn(encryptedSecret)
        `when`(cipher.decrypt(encryptedSecret)).thenReturn(testSecret)
        `when`(userRepository.getTotpLastUsedStep(u.id)).thenReturn(null)
        `when`(userRepository.updateTotpLastUsedStep(eqArg(u.id), ArgumentMatchers.anyLong())).thenReturn(true)

        val validCode = computeValidCode(testSecret)
        val result = service().verifyChallengeCode(u.id, validCode)

        assertTrue(result)
        verify(userRepository).updateTotpLastUsedStep(eqArg(u.id), ArgumentMatchers.anyLong())
    }

    @Test
    fun `verifyChallengeCode returns false for an invalid code`() {
        val u = user(totpEnabled = true)
        `when`(userRepository.getActiveTotpSecret(u.id)).thenReturn(encryptedSecret)
        `when`(cipher.decrypt(encryptedSecret)).thenReturn(testSecret)
        `when`(twoFactorRepository.fetchUnusedRecoveryCodeHashes(u.id)).thenReturn(emptyList())

        val result = service().verifyChallengeCode(u.id, "000000")

        assertFalse(result)
    }

    @Test
    fun `verifyChallengeCode returns false when active secret is missing`() {
        val u = user(totpEnabled = true)
        `when`(userRepository.getActiveTotpSecret(u.id)).thenReturn(null)

        val result = service().verifyChallengeCode(u.id, "123456")

        assertFalse(result)
    }

    @Test
    fun `verifyChallengeCode rejects replay of a previously used time step`() {
        val u = user(totpEnabled = true)
        val currentStep = timeProvider.time / 30
        `when`(userRepository.getActiveTotpSecret(u.id)).thenReturn(encryptedSecret)
        `when`(cipher.decrypt(encryptedSecret)).thenReturn(testSecret)
        `when`(userRepository.getTotpLastUsedStep(u.id)).thenReturn(currentStep + 1)

        val validCode = computeValidCode(testSecret)
        val result = service().verifyChallengeCode(u.id, validCode)

        assertFalse(result)
        verify(userRepository, never()).updateTotpLastUsedStep(anyArg<UUID>(), ArgumentMatchers.anyLong())
    }

    @Test
    fun `verifyChallengeCode returns true for a valid recovery code and marks it used`() {
        val u = user(totpEnabled = true)
        val recoveryCode = "1234-5678-ABCD-EFGH"
        `when`(userRepository.getActiveTotpSecret(u.id)).thenReturn(encryptedSecret)
        `when`(cipher.decrypt(encryptedSecret)).thenReturn(testSecret)
        `when`(twoFactorRepository.fetchUnusedRecoveryCodeHashes(u.id)).thenReturn(listOf("hashX"))
        `when`(passwordEncoder.matches(eqArg(recoveryCode), eqArg("hashX"))).thenReturn(true)
        `when`(twoFactorRepository.markRecoveryCodeUsed(u.id, "hashX")).thenReturn(true)

        val result = service().verifyChallengeCode(u.id, recoveryCode)

        assertTrue(result)
        verify(twoFactorRepository).markRecoveryCodeUsed(u.id, "hashX")
    }

    private fun computeValidCode(secret: String): String {
        val counter = timeProvider.time / 30
        return codeGenerator.generate(secret, counter)
    }
}
