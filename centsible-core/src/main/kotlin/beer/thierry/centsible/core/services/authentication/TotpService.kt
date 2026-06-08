package beer.thierry.centsible.core.services.authentication

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.auth.RecoveryCodesDTO
import beer.thierry.centsible.api.model.auth.TotpEnrollmentDTO
import beer.thierry.centsible.api.model.auth.TotpStatusDTO
import beer.thierry.centsible.api.repository.IMfaRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.authentication.ITotpService
import dev.samstevens.totp.code.CodeVerifier
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.qr.QrData
import dev.samstevens.totp.recovery.RecoveryCodeGenerator
import dev.samstevens.totp.secret.DefaultSecretGenerator
import dev.samstevens.totp.time.SystemTimeProvider
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TotpService(
    private val userRepository: IUserRepository,
    private val mfaRepository: IMfaRepository,
    private val cipher: TotpSecretCipher,
    private val passwordEncoder: PasswordEncoder,
) : ITotpService {
    private val log = LoggerFactory.getLogger(TotpService::class.java)

    private val secretGenerator = DefaultSecretGenerator()
    private val recoveryCodeGenerator = RecoveryCodeGenerator()
    private val timeProvider = SystemTimeProvider()

    private val codeVerifier: CodeVerifier = DefaultCodeVerifier(DefaultCodeGenerator(), timeProvider).apply {
        setTimePeriod(TIME_PERIOD_SECONDS)
        setAllowedTimePeriodDiscrepancy(1)
    }

    override fun status(userId: UUID): TotpStatusDTO {
        val user = userRepository.findUserById(userId)
            ?: throw LocalizedException.NotFound("error.user.notFound")
        val remaining = if (user.totpEnabled) mfaRepository.fetchUnusedRecoveryCodeHashes(userId).size else 0
        return TotpStatusDTO(enabled = user.totpEnabled, recoveryCodesRemaining = remaining)
    }

    override fun beginEnrollment(userId: UUID, accountName: String): TotpEnrollmentDTO {
        val user = userRepository.findUserById(userId)
            ?: throw LocalizedException.NotFound("error.user.notFound")
        if (user.totpEnabled) throw LocalizedException.Conflict("error.totp.alreadyEnabled")

        val secret = secretGenerator.generate()
        if (!userRepository.savePendingTotpSecret(userId, cipher.encrypt(secret))) {
            throw LocalizedException.InternalError("error.totp.enrollFailed")
        }

        val otpauthUri = QrData.Builder()
            .label(accountName)
            .secret(secret)
            .issuer(ISSUER)
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(TIME_PERIOD_SECONDS)
            .build()
            .uri

        log.info("Started TOTP enrollment userId={}", userId)
        return TotpEnrollmentDTO(otpauthUri = otpauthUri, secret = secret)
    }

    override fun confirmEnrollment(userId: UUID, code: String): RecoveryCodesDTO {
        val pending = userRepository.getPendingTotpSecret(userId)
            ?: throw LocalizedException.BadRequest("error.totp.noPendingEnrollment")
        val secret = cipher.decrypt(pending)
        if (!codeVerifier.isValidCode(secret, code.trim())) {
            log.warn("TOTP enrollment confirmation failed: bad code userId={}", userId)
            throw LocalizedException.BadRequest("error.totp.invalidCode")
        }

        if (!userRepository.activateTotp(userId, cipher.encrypt(secret))) {
            throw LocalizedException.InternalError("error.totp.enrollFailed")
        }
        userRepository.updateTotpLastUsedStep(userId, currentStep())

        val plainCodes = generateAndStoreRecoveryCodes(userId)

        log.info("Enabled TOTP 2FA userId={}", userId)
        return RecoveryCodesDTO(recoveryCodes = plainCodes)
    }

    override fun disable(userId: UUID, code: String) {
        val user = userRepository.findUserById(userId)
            ?: throw LocalizedException.NotFound("error.user.notFound")
        if (!user.totpEnabled) throw LocalizedException.BadRequest("error.totp.notEnabled")

        if (!verifyCodeForUser(userId, code)) {
            log.warn("TOTP disable rejected: bad code userId={}", userId)
            throw LocalizedException.BadRequest("error.totp.invalidCode")
        }
        userRepository.disableTotp(userId)
        mfaRepository.deleteRecoveryCodes(userId)
        log.info("Disabled TOTP 2FA userId={}", userId)
    }

    override fun cancelEnrollment(userId: UUID) {
        if (userRepository.clearPendingTotpSecret(userId)) {
            log.info("Cancelled TOTP enrollment userId={}", userId)
        }
    }

    override fun regenerateRecoveryCodes(userId: UUID, code: String): RecoveryCodesDTO {
        val user = userRepository.findUserById(userId)
            ?: throw LocalizedException.NotFound("error.user.notFound")
        if (!user.totpEnabled) throw LocalizedException.BadRequest("error.totp.notEnabled")
        if (!verifyCodeForUser(userId, code)) {
            log.warn("Recovery-code regeneration rejected: bad code userId={}", userId)
            throw LocalizedException.BadRequest("error.totp.invalidCode")
        }
        val codes = generateAndStoreRecoveryCodes(userId)
        log.info("Regenerated recovery codes userId={}", userId)
        return RecoveryCodesDTO(recoveryCodes = codes)
    }

    /** Generates a fresh set of recovery codes, replacing any existing ones, and returns the plaintext. */
    private fun generateAndStoreRecoveryCodes(userId: UUID): List<String> {
        val plainCodes: List<String> = recoveryCodeGenerator.generateCodes(RECOVERY_CODE_COUNT).toList()
        val hashes = plainCodes.map {
            passwordEncoder.encode(it) ?: throw LocalizedException.InternalError("error.totp.enrollFailed")
        }
        mfaRepository.replaceRecoveryCodes(userId, hashes)
        return plainCodes
    }

    override fun verifyChallengeCode(userId: UUID, code: String): Boolean = verifyCodeForUser(userId, code)

    /**
     * Verifies a TOTP code (with replay rejection) or, failing that, an unused recovery code.
     * Returns true and records consumption on success. Shared by the login challenge and disable.
     */
    private fun verifyCodeForUser(userId: UUID, code: String): Boolean {
        val trimmed = code.trim()
        val encrypted = userRepository.getActiveTotpSecret(userId) ?: return false
        val secret = cipher.decrypt(encrypted)

        if (codeVerifier.isValidCode(secret, trimmed)) {
            val current = currentStep()
            val lastUsed = userRepository.getTotpLastUsedStep(userId)
            if (lastUsed != null && current <= lastUsed) {
                log.warn("TOTP replay rejected userId={} step={}", userId, current)
                return false
            }
            userRepository.updateTotpLastUsedStep(userId, current)
            return true
        }

        val match = mfaRepository.fetchUnusedRecoveryCodeHashes(userId)
            .firstOrNull { passwordEncoder.matches(trimmed, it) }
            ?: return false
        val consumed = mfaRepository.markRecoveryCodeUsed(userId, match)
        if (consumed) log.info("Recovery code consumed userId={}", userId)
        return consumed
    }

    private fun currentStep(): Long = timeProvider.time / TIME_PERIOD_SECONDS

    private companion object {
        const val ISSUER = "Centsible"
        const val TIME_PERIOD_SECONDS = 30
        const val RECOVERY_CODE_COUNT = 16
    }
}
