package beer.thierry.centsible.core.services.integrations

import beer.thierry.centsible.api.model.integrations.AuthType
import beer.thierry.centsible.api.model.integrations.ConfigField
import beer.thierry.centsible.api.model.integrations.FieldType
import beer.thierry.centsible.api.model.integrations.ProviderConnectionDTO
import beer.thierry.centsible.api.model.integrations.ProviderConnectionForm
import beer.thierry.centsible.api.model.integrations.ProviderConnectionStatus
import beer.thierry.centsible.api.model.integrations.ProviderContext
import beer.thierry.centsible.api.model.integrations.ProviderDescriptor
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IProviderConnectionsRepository
import beer.thierry.centsible.api.repository.ProviderConnectionRecord
import beer.thierry.centsible.api.services.integrations.IProviderConnectionService
import beer.thierry.centsible.api.services.integrations.IProviderRegistry
import beer.thierry.centsible.api.services.integrations.ProviderModule
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProviderConnectionService(
    private val registry: IProviderRegistry,
    private val repository: IProviderConnectionsRepository,
    private val cipher: CredentialCipher,
) : IProviderConnectionService {

    override fun fetchAllConnections(authenticatedUser: UserDTO): List<ProviderConnectionDTO> =
        repository.fetchAll(authenticatedUser).map { it.toDTO() }

    override fun fetchConnectionById(authenticatedUser: UserDTO, id: UUID): ProviderConnectionDTO? =
        repository.fetchById(authenticatedUser, id)?.toDTO()

    @Transactional
    override fun createConnection(authenticatedUser: UserDTO, form: ProviderConnectionForm): ProviderConnectionDTO {
        val module = registry.getModule(form.providerKey)
            ?: throw IllegalArgumentException("Unknown provider key: ${form.providerKey}")
        val (config, credentials) = splitAndValidate(module.descriptor, form.values, partial = false)
        runProviderTest(module, authenticatedUser.id, UUID.randomUUID(), form.displayName, config, credentials)
        val initialStatus = when (module.descriptor.authType) {
            AuthType.OAUTH2 -> ProviderConnectionStatus.NEW
            else -> ProviderConnectionStatus.ACTIVE
        }
        return repository.create(
            userId = authenticatedUser.id,
            providerKey = form.providerKey,
            displayName = form.displayName,
            status = initialStatus,
            config = config,
            credentials = cipher.encrypt(credentials),
        ).toDTO()
    }

    @Transactional
    override fun updateConnection(
        authenticatedUser: UserDTO,
        id: UUID,
        form: ProviderConnectionForm,
    ): ProviderConnectionDTO? {
        val existing = repository.fetchById(authenticatedUser, id) ?: return null
        val module = registry.getModule(existing.providerKey)
            ?: throw IllegalArgumentException("Provider '${existing.providerKey}' is no longer registered")
        // partial=true: omitted secret fields keep their current value.
        val (newConfig, submittedSecrets) = splitAndValidate(module.descriptor, form.values, partial = true)
        val existingSecrets = cipher.decrypt(repository.fetchEncryptedCredentialsById(authenticatedUser, id))
        val mergedSecrets = existingSecrets.toMutableMap().apply { putAll(submittedSecrets) }
        runProviderTest(module, authenticatedUser.id, existing.id, form.displayName, newConfig, mergedSecrets)
        return repository.update(
            authenticatedUser = authenticatedUser,
            id = id,
            displayName = form.displayName,
            status = ProviderConnectionStatus.ACTIVE,
            config = newConfig,
            credentials = cipher.encrypt(mergedSecrets),
        )?.toDTO()
    }

    override fun deleteConnection(authenticatedUser: UserDTO, id: UUID): Boolean =
        repository.delete(authenticatedUser, id)

    override fun triggerSync(authenticatedUser: UserDTO, id: UUID): Boolean =
        repository.requeueForSync(authenticatedUser, id)

    private fun runProviderTest(
        module: ProviderModule,
        userId: UUID,
        connectionId: UUID,
        displayName: String,
        config: Map<String, Any?>,
        credentials: Map<String, String>,
    ) {
        try {
            module.testConnection(
                ProviderContext(
                    userId = userId,
                    connectionId = connectionId,
                    displayName = displayName,
                    config = config,
                    credentials = credentials,
                )
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Connection test failed: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /**
     * Validates form values against the descriptor and splits them into:
     *  - config (plaintext, JSONB) for non-secret fields
     *  - secrets (encrypted, BYTEA) for secret fields
     *
     * @param partial when true, missing required fields are allowed (used for PUT where the client
     *   may omit unchanged secret fields).
     */
    private fun splitAndValidate(
        descriptor: ProviderDescriptor,
        values: Map<String, Any?>,
        partial: Boolean,
    ): Pair<Map<String, Any?>, Map<String, String>> {
        val knownFields = descriptor.configFields.associateBy { it.name }
        val unknown = values.keys - knownFields.keys
        if (unknown.isNotEmpty()) {
            throw IllegalArgumentException("Unknown fields for provider ${descriptor.key}: $unknown")
        }
        val config = mutableMapOf<String, Any?>()
        val secrets = mutableMapOf<String, String>()
        for (field in descriptor.configFields) {
            val raw = values[field.name]
            val isMissing = raw == null || (raw is String && raw.isBlank())
            if (isMissing) {
                if (field.required && !partial) {
                    throw IllegalArgumentException("Missing required field: ${field.name}")
                }
                continue
            }
            val coerced = coerce(field, raw!!)
            if (field.secret) secrets[field.name] = coerced.toString()
            else config[field.name] = coerced
        }
        return config to secrets
    }

    private fun coerce(field: ConfigField, raw: Any): Any = when (field.type) {
        FieldType.STRING, FieldType.MULTILINE -> raw.toString()
        FieldType.NUMBER -> when (raw) {
            is Number -> raw
            is String -> raw.toBigDecimalOrNull()
                ?: throw IllegalArgumentException("Field '${field.name}' must be a number")
            else -> throw IllegalArgumentException("Field '${field.name}' must be a number")
        }
        FieldType.BOOLEAN -> when (raw) {
            is Boolean -> raw
            is String -> raw.lowercase().let {
                when (it) {
                    "true" -> true; "false" -> false
                    else -> throw IllegalArgumentException("Field '${field.name}' must be true or false")
                }
            }
            else -> throw IllegalArgumentException("Field '${field.name}' must be true or false")
        }
        FieldType.SELECT -> {
            val str = raw.toString()
            val allowed = field.options.map { it.value }.toSet()
            if (allowed.isNotEmpty() && str !in allowed) {
                throw IllegalArgumentException("Field '${field.name}' must be one of $allowed")
            }
            str
        }
    }

    private fun ProviderConnectionRecord.toDTO(): ProviderConnectionDTO = ProviderConnectionDTO(
        id = id,
        providerKey = providerKey,
        displayName = displayName,
        status = status,
        config = config,
        lastSyncAt = lastSyncAt,
        lastError = lastError,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
    )
}
