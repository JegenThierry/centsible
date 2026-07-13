package beer.thierry.centsible.core.services.integrations

import beer.thierry.centsible.api.model.integrations.AuthType
import beer.thierry.centsible.api.model.integrations.ConfigField
import beer.thierry.centsible.api.model.integrations.FieldType
import beer.thierry.centsible.api.model.integrations.ProviderConnectionDTO
import beer.thierry.centsible.api.model.integrations.ProviderConnectionForm
import beer.thierry.centsible.api.model.integrations.ProviderConnectionStatus
import beer.thierry.centsible.api.model.integrations.ProviderContext
import beer.thierry.centsible.api.model.integrations.ProviderDescriptor
import beer.thierry.centsible.api.model.integrations.RemoteOptionsRequest
import beer.thierry.centsible.api.model.integrations.SelectOption
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IProviderConnectionsRepository
import beer.thierry.centsible.api.services.integrations.IProviderConnectionService
import beer.thierry.centsible.api.services.integrations.IProviderRegistry
import beer.thierry.centsible.api.services.integrations.IRemoteOptionsProvider
import beer.thierry.centsible.api.services.integrations.ProviderModule
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProviderConnectionService(
    private val registry: IProviderRegistry,
    private val repository: IProviderConnectionsRepository,
    private val cipher: CredentialCipher,
) : IProviderConnectionService {

    private val log = LoggerFactory.getLogger(ProviderConnectionService::class.java)

    override fun fetchAllConnections(authenticatedUser: UserDTO): List<ProviderConnectionDTO> =
        repository.fetchAll(authenticatedUser).map(ConnectionMappersImpl::toDTO)

    override fun fetchConnectionById(authenticatedUser: UserDTO, id: UUID): ProviderConnectionDTO? =
        repository.fetchById(authenticatedUser, id)?.let(ConnectionMappersImpl::toDTO)

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
        val record = repository.create(
            userId = authenticatedUser.id,
            providerKey = form.providerKey,
            displayName = form.displayName,
            status = initialStatus,
            config = config,
            credentials = cipher.encrypt(credentials),
        )
        log.info(
            "Created provider connection id={} userId={} provider={} status={}",
            record.id, authenticatedUser.id, form.providerKey, initialStatus,
        )
        return ConnectionMappersImpl.toDTO(record)
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
        val (newConfig, submittedSecrets) = splitAndValidate(module.descriptor, form.values, partial = true)
        val existingSecrets = cipher.decrypt(repository.fetchEncryptedCredentialsById(authenticatedUser, id))
        val mergedSecrets = existingSecrets + submittedSecrets
        runProviderTest(module, authenticatedUser.id, existing.id, form.displayName, newConfig, mergedSecrets)
        val updated = repository.update(
            authenticatedUser = authenticatedUser,
            id = id,
            displayName = form.displayName,
            status = ProviderConnectionStatus.ACTIVE,
            config = newConfig,
            credentials = cipher.encrypt(mergedSecrets),
        )?.let(ConnectionMappersImpl::toDTO)
        if (updated != null) {
            log.info(
                "Updated provider connection id={} userId={} provider={} secretFieldsChanged={}",
                id, authenticatedUser.id, existing.providerKey, submittedSecrets.keys.sorted(),
            )
        }
        return updated
    }

    override fun deleteConnection(authenticatedUser: UserDTO, id: UUID): Boolean {
        val deleted = repository.delete(authenticatedUser, id)
        if (deleted) {
            log.info("Deleted provider connection id={} userId={}", id, authenticatedUser.id)
        }
        return deleted
    }

    override fun triggerSync(authenticatedUser: UserDTO, id: UUID): Boolean {
        val requeued = repository.requeueForSync(authenticatedUser, id)
        if (requeued) {
            log.info("Requeued provider connection for sync id={} userId={}", id, authenticatedUser.id)
        }
        return requeued
    }

    override fun fetchRemoteOptions(
        authenticatedUser: UserDTO,
        providerKey: String,
        fieldName: String,
        query: String?,
        values: Map<String, Any?>,
    ): List<SelectOption> {
        val module = registry.getModule(providerKey)
            ?: throw IllegalArgumentException("Unknown provider key: $providerKey")
        val field = module.descriptor.configFields.firstOrNull { it.name == fieldName }
            ?: throw IllegalArgumentException("Unknown field '$fieldName' for provider $providerKey")
        require(field.type == FieldType.SELECT_REMOTE) {
            "Field '$fieldName' is not a SELECT_REMOTE field"
        }
        require(module is IRemoteOptionsProvider) {
            "Provider '$providerKey' does not provide remote options"
        }
        val ctx = ProviderContext(
            userId = authenticatedUser.id,
            connectionId = UUID(0, 0),
            displayName = "remote-options-lookup",
            config = values.filterKeys { it != fieldName },
            credentials = emptyMap(),
        )
        return module.fetchOptions(ctx, RemoteOptionsRequest(fieldName, query, values))
    }

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
            log.warn(
                "Provider connection test failed userId={} connectionId={} provider={}",
                userId, connectionId, module.descriptor.key, e,
            )
            throw IllegalArgumentException("Connection test failed: ${e.message ?: e.javaClass.simpleName}", e)
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
            if (field.type == FieldType.OAUTH_LAUNCH) continue
            val raw = values[field.name]
            val isMissing = raw == null || (raw is String && raw.isBlank())
            if (isMissing) {
                if (field.required && !partial) {
                    throw IllegalArgumentException("Missing required field: ${field.name}")
                }
                continue
            }
            val coerced = coerce(field, raw)
            if (field.secret) secrets[field.name] = coerced.toString()
            else config[field.name] = coerced
        }
        return config to secrets
    }

    private fun coerce(field: ConfigField, raw: Any): Any = when (field.type) {
        FieldType.STRING, FieldType.MULTILINE, FieldType.SELECT_REMOTE -> raw.toString()
        FieldType.NUMBER -> when (raw) {
            is Number -> raw
            is String -> raw.toBigDecimalOrNull()
                ?: throw IllegalArgumentException("Field '${field.name}' must be a number")
            else -> throw IllegalArgumentException("Field '${field.name}' must be a number")
        }
        FieldType.BOOLEAN -> when (raw) {
            is Boolean -> raw
            is String if raw.equals("true", ignoreCase = true) -> true
            is String if raw.equals("false", ignoreCase = true) -> false
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
        FieldType.OAUTH_LAUNCH -> ""
    }

}
