package beer.thierry.budgetplanner.jooq.repository

import beer.thierry.budgetplanner.api.model.integrations.ProviderConnectionStatus
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.repository.IProviderConnectionsRepository
import beer.thierry.budgetplanner.api.repository.ProviderConnectionRecord
import beer.thierry.budgetplanner.api.repository.ProviderSyncCandidate
import beer.thierry.jooq.generated.tables.references.PROVIDER_CONNECTIONS
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.Record
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID
import beer.thierry.jooq.generated.enums.ProviderConnectionStatus as JooqStatus

@Repository
class ProviderConnectionsRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) : IProviderConnectionsRepository {

    private val configType = object : TypeReference<Map<String, Any?>>() {}

    override fun fetchAll(authenticatedUser: UserDTO): List<ProviderConnectionRecord> =
        dsl.select(*RECORD_COLUMNS)
            .from(PROVIDER_CONNECTIONS)
            .where(PROVIDER_CONNECTIONS.USER_ID.eq(authenticatedUser.id))
            .orderBy(PROVIDER_CONNECTIONS.CREATED_AT.desc())
            .fetch { it.toRecord() }

    override fun fetchById(authenticatedUser: UserDTO, id: UUID): ProviderConnectionRecord? =
        dsl.select(*RECORD_COLUMNS)
            .from(PROVIDER_CONNECTIONS)
            .where(PROVIDER_CONNECTIONS.ID.eq(id).and(PROVIDER_CONNECTIONS.USER_ID.eq(authenticatedUser.id)))
            .fetchOne()?.toRecord()

    override fun fetchEncryptedCredentialsById(authenticatedUser: UserDTO, id: UUID): ByteArray? =
        dsl.select(PROVIDER_CONNECTIONS.CREDENTIALS)
            .from(PROVIDER_CONNECTIONS)
            .where(PROVIDER_CONNECTIONS.ID.eq(id).and(PROVIDER_CONNECTIONS.USER_ID.eq(authenticatedUser.id)))
            .fetchOne(PROVIDER_CONNECTIONS.CREDENTIALS)

    @Transactional
    override fun create(
        userId: UUID,
        providerKey: String,
        displayName: String,
        status: ProviderConnectionStatus,
        config: Map<String, Any?>,
        credentials: ByteArray?,
    ): ProviderConnectionRecord {
        val row = dsl.insertInto(PROVIDER_CONNECTIONS)
            .set(PROVIDER_CONNECTIONS.USER_ID, userId)
            .set(PROVIDER_CONNECTIONS.PROVIDER_KEY, providerKey)
            .set(PROVIDER_CONNECTIONS.DISPLAY_NAME, displayName)
            .set(PROVIDER_CONNECTIONS.STATUS, status.toJooq())
            .set(PROVIDER_CONNECTIONS.CONFIG, JSONB.valueOf(objectMapper.writeValueAsString(config)))
            .set(PROVIDER_CONNECTIONS.CREDENTIALS, credentials)
            .returning(*RECORD_COLUMNS)
            .fetchOne() ?: error("Failed to insert provider_connections row")
        return row.toRecord()
    }

    @Transactional
    override fun update(
        authenticatedUser: UserDTO,
        id: UUID,
        displayName: String,
        status: ProviderConnectionStatus,
        config: Map<String, Any?>,
        credentials: ByteArray?,
    ): ProviderConnectionRecord? {
        val now = OffsetDateTime.now()
        return dsl.update(PROVIDER_CONNECTIONS)
            .set(PROVIDER_CONNECTIONS.DISPLAY_NAME, displayName)
            .set(PROVIDER_CONNECTIONS.STATUS, status.toJooq())
            .set(PROVIDER_CONNECTIONS.CONFIG, JSONB.valueOf(objectMapper.writeValueAsString(config)))
            .set(PROVIDER_CONNECTIONS.CREDENTIALS, credentials)
            .set(PROVIDER_CONNECTIONS.MODIFIED_AT, now)
            .where(PROVIDER_CONNECTIONS.ID.eq(id).and(PROVIDER_CONNECTIONS.USER_ID.eq(authenticatedUser.id)))
            .returning(*RECORD_COLUMNS)
            .fetchOne()?.toRecord()
    }

    override fun delete(authenticatedUser: UserDTO, id: UUID): Boolean =
        dsl.deleteFrom(PROVIDER_CONNECTIONS)
            .where(PROVIDER_CONNECTIONS.ID.eq(id).and(PROVIDER_CONNECTIONS.USER_ID.eq(authenticatedUser.id)))
            .execute() > 0

    @Transactional
    override fun requeueForSync(authenticatedUser: UserDTO, id: UUID): Boolean {
        val now = OffsetDateTime.now()
        return dsl.update(PROVIDER_CONNECTIONS)
            .setNull(PROVIDER_CONNECTIONS.LAST_SYNC_AT)
            .set(PROVIDER_CONNECTIONS.STATUS, JooqStatus.ACTIVE)
            .setNull(PROVIDER_CONNECTIONS.LAST_ERROR)
            .set(PROVIDER_CONNECTIONS.MODIFIED_AT, now)
            .where(
                PROVIDER_CONNECTIONS.ID.eq(id)
                    .and(PROVIDER_CONNECTIONS.USER_ID.eq(authenticatedUser.id))
                    .and(PROVIDER_CONNECTIONS.STATUS.ne(JooqStatus.REVOKED))
            )
            .execute() > 0
    }

    @Transactional
    override fun claimNextDueForSync(
        workerId: String,
        leaseTimeoutSeconds: Long,
        syncIntervalSeconds: Long,
    ): ProviderSyncCandidate? {
        val now = OffsetDateTime.now()
        val syncCutoff = now.minusSeconds(syncIntervalSeconds)
        val leaseCutoff = now.minusSeconds(leaseTimeoutSeconds)

        val candidate = dsl.select(PROVIDER_CONNECTIONS.ID)
            .from(PROVIDER_CONNECTIONS)
            .where(
                PROVIDER_CONNECTIONS.STATUS.eq(JooqStatus.ACTIVE)
                    .and(
                        PROVIDER_CONNECTIONS.LAST_SYNC_AT.isNull
                            .or(PROVIDER_CONNECTIONS.LAST_SYNC_AT.lt(syncCutoff))
                    )
                    .and(
                        PROVIDER_CONNECTIONS.LOCKED_AT.isNull
                            .or(PROVIDER_CONNECTIONS.LOCKED_AT.lt(leaseCutoff))
                    )
            )
            .orderBy(PROVIDER_CONNECTIONS.LAST_SYNC_AT.asc().nullsFirst())
            .limit(1)
            .forUpdate().skipLocked()

        return dsl.update(PROVIDER_CONNECTIONS)
            .set(PROVIDER_CONNECTIONS.LOCKED_AT, now)
            .set(PROVIDER_CONNECTIONS.LOCKED_BY, workerId)
            .set(PROVIDER_CONNECTIONS.ATTEMPT_COUNT, PROVIDER_CONNECTIONS.ATTEMPT_COUNT.plus(1))
            .set(PROVIDER_CONNECTIONS.MODIFIED_AT, now)
            .where(PROVIDER_CONNECTIONS.ID.`in`(candidate))
            .returning(*CANDIDATE_COLUMNS)
            .fetchOne()?.toCandidate()
    }

    override fun markSyncSuccess(id: UUID, cursor: String?) {
        val now = OffsetDateTime.now()
        dsl.update(PROVIDER_CONNECTIONS)
            .set(PROVIDER_CONNECTIONS.LAST_SYNC_AT, now)
            .set(PROVIDER_CONNECTIONS.LAST_SYNC_CURSOR, cursor)
            .setNull(PROVIDER_CONNECTIONS.LAST_ERROR)
            .setNull(PROVIDER_CONNECTIONS.LOCKED_AT)
            .setNull(PROVIDER_CONNECTIONS.LOCKED_BY)
            .set(PROVIDER_CONNECTIONS.MODIFIED_AT, now)
            .where(PROVIDER_CONNECTIONS.ID.eq(id))
            .execute()
    }

    override fun markSyncError(id: UUID, errorMessage: String) {
        val now = OffsetDateTime.now()
        dsl.update(PROVIDER_CONNECTIONS)
            .set(PROVIDER_CONNECTIONS.LAST_ERROR, errorMessage.take(2000))
            .setNull(PROVIDER_CONNECTIONS.LOCKED_AT)
            .setNull(PROVIDER_CONNECTIONS.LOCKED_BY)
            .set(PROVIDER_CONNECTIONS.MODIFIED_AT, now)
            .where(PROVIDER_CONNECTIONS.ID.eq(id))
            .execute()
    }

    private fun Record.toRecord(): ProviderConnectionRecord = ProviderConnectionRecord(
        id = this[PROVIDER_CONNECTIONS.ID]!!,
        providerKey = this[PROVIDER_CONNECTIONS.PROVIDER_KEY]!!,
        displayName = this[PROVIDER_CONNECTIONS.DISPLAY_NAME]!!,
        status = this[PROVIDER_CONNECTIONS.STATUS]!!.toApi(),
        config = readConfig(this[PROVIDER_CONNECTIONS.CONFIG]),
        lastSyncAt = this[PROVIDER_CONNECTIONS.LAST_SYNC_AT],
        lastError = this[PROVIDER_CONNECTIONS.LAST_ERROR],
        createdAt = this[PROVIDER_CONNECTIONS.CREATED_AT]!!,
        modifiedAt = this[PROVIDER_CONNECTIONS.MODIFIED_AT]!!,
    )

    private fun Record.toCandidate(): ProviderSyncCandidate = ProviderSyncCandidate(
        connectionId = this[PROVIDER_CONNECTIONS.ID]!!,
        userId = this[PROVIDER_CONNECTIONS.USER_ID]!!,
        providerKey = this[PROVIDER_CONNECTIONS.PROVIDER_KEY]!!,
        displayName = this[PROVIDER_CONNECTIONS.DISPLAY_NAME]!!,
        config = readConfig(this[PROVIDER_CONNECTIONS.CONFIG]),
        encryptedCredentials = this[PROVIDER_CONNECTIONS.CREDENTIALS],
        lastSyncCursor = this[PROVIDER_CONNECTIONS.LAST_SYNC_CURSOR],
    )

    private fun readConfig(jsonb: JSONB?): Map<String, Any?> =
        if (jsonb == null) emptyMap() else objectMapper.readValue(jsonb.data(), configType)

    companion object {
        private val RECORD_COLUMNS = arrayOf(
            PROVIDER_CONNECTIONS.ID,
            PROVIDER_CONNECTIONS.PROVIDER_KEY,
            PROVIDER_CONNECTIONS.DISPLAY_NAME,
            PROVIDER_CONNECTIONS.STATUS,
            PROVIDER_CONNECTIONS.CONFIG,
            PROVIDER_CONNECTIONS.LAST_SYNC_AT,
            PROVIDER_CONNECTIONS.LAST_ERROR,
            PROVIDER_CONNECTIONS.CREATED_AT,
            PROVIDER_CONNECTIONS.MODIFIED_AT,
        )
        private val CANDIDATE_COLUMNS = arrayOf(
            PROVIDER_CONNECTIONS.ID,
            PROVIDER_CONNECTIONS.USER_ID,
            PROVIDER_CONNECTIONS.PROVIDER_KEY,
            PROVIDER_CONNECTIONS.DISPLAY_NAME,
            PROVIDER_CONNECTIONS.CONFIG,
            PROVIDER_CONNECTIONS.CREDENTIALS,
            PROVIDER_CONNECTIONS.LAST_SYNC_CURSOR,
        )
    }
}

internal fun ProviderConnectionStatus.toJooq(): JooqStatus = when (this) {
    ProviderConnectionStatus.NEW -> JooqStatus.NEW
    ProviderConnectionStatus.ACTIVE -> JooqStatus.ACTIVE
    ProviderConnectionStatus.ERROR -> JooqStatus.ERROR
    ProviderConnectionStatus.REVOKED -> JooqStatus.REVOKED
}

internal fun JooqStatus.toApi(): ProviderConnectionStatus = when (this) {
    JooqStatus.NEW -> ProviderConnectionStatus.NEW
    JooqStatus.ACTIVE -> ProviderConnectionStatus.ACTIVE
    JooqStatus.ERROR -> ProviderConnectionStatus.ERROR
    JooqStatus.REVOKED -> ProviderConnectionStatus.REVOKED
}
