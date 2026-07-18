package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.repository.IProviderConnectionAccountsRepository
import beer.thierry.centsible.api.repository.ProviderConnectionAccountRecord
import beer.thierry.jooq.generated.tables.references.PROVIDER_CONNECTION_ACCOUNTS
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.Record
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class ProviderConnectionAccountsRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) : IProviderConnectionAccountsRepository {

    private val metadataType = object : TypeReference<Map<String, Any?>>() {}

    override fun fetchAllForConnection(providerConnectionId: UUID): List<ProviderConnectionAccountRecord> =
        dsl.select(*COLUMNS)
            .from(PROVIDER_CONNECTION_ACCOUNTS)
            .where(PROVIDER_CONNECTION_ACCOUNTS.PROVIDER_CONNECTION_ID.eq(providerConnectionId))
            .orderBy(PROVIDER_CONNECTION_ACCOUNTS.CREATED_AT.asc())
            .fetch { it.toRecord() }

    override fun fetchByExternalId(
        providerConnectionId: UUID,
        externalAccountId: String,
    ): ProviderConnectionAccountRecord? =
        dsl.select(*COLUMNS)
            .from(PROVIDER_CONNECTION_ACCOUNTS)
            .where(
                PROVIDER_CONNECTION_ACCOUNTS.PROVIDER_CONNECTION_ID.eq(providerConnectionId)
                    .and(PROVIDER_CONNECTION_ACCOUNTS.EXTERNAL_ACCOUNT_ID.eq(externalAccountId))
            )
            .fetchOne()?.toRecord()

    @Transactional
    override fun upsert(
        providerConnectionId: UUID,
        externalAccountId: String,
        accountId: UUID?,
        externalMetadata: Map<String, Any?>,
    ): ProviderConnectionAccountRecord {
        val metadataJson = JSONB.valueOf(objectMapper.writeValueAsString(externalMetadata))
        val now = OffsetDateTime.now()
        val row = dsl.insertInto(PROVIDER_CONNECTION_ACCOUNTS)
            .set(PROVIDER_CONNECTION_ACCOUNTS.PROVIDER_CONNECTION_ID, providerConnectionId)
            .set(PROVIDER_CONNECTION_ACCOUNTS.EXTERNAL_ACCOUNT_ID, externalAccountId)
            .set(PROVIDER_CONNECTION_ACCOUNTS.ACCOUNT_ID, accountId)
            .set(PROVIDER_CONNECTION_ACCOUNTS.EXTERNAL_METADATA, metadataJson)
            .onConflict(
                PROVIDER_CONNECTION_ACCOUNTS.PROVIDER_CONNECTION_ID,
                PROVIDER_CONNECTION_ACCOUNTS.EXTERNAL_ACCOUNT_ID,
            )
            .doUpdate()
            .set(PROVIDER_CONNECTION_ACCOUNTS.ACCOUNT_ID, accountId)
            .set(PROVIDER_CONNECTION_ACCOUNTS.EXTERNAL_METADATA, metadataJson)
            .set(PROVIDER_CONNECTION_ACCOUNTS.MODIFIED_AT, now)
            .returning(*COLUMNS)
            .fetchOne() ?: error("Failed to upsert provider_connection_accounts row")
        return row.toRecord()
    }

    private fun Record.toRecord(): ProviderConnectionAccountRecord = ProviderConnectionAccountRecord(
        id = this[PROVIDER_CONNECTION_ACCOUNTS.ID]!!,
        providerConnectionId = this[PROVIDER_CONNECTION_ACCOUNTS.PROVIDER_CONNECTION_ID]!!,
        accountId = this[PROVIDER_CONNECTION_ACCOUNTS.ACCOUNT_ID],
        externalAccountId = this[PROVIDER_CONNECTION_ACCOUNTS.EXTERNAL_ACCOUNT_ID]!!,
        externalMetadata = readMetadata(this[PROVIDER_CONNECTION_ACCOUNTS.EXTERNAL_METADATA]),
    )

    private fun readMetadata(jsonb: JSONB?): Map<String, Any?> =
        if (jsonb == null) emptyMap() else objectMapper.readValue(jsonb.data(), metadataType)

    private companion object {
        val COLUMNS = arrayOf(
            PROVIDER_CONNECTION_ACCOUNTS.ID,
            PROVIDER_CONNECTION_ACCOUNTS.PROVIDER_CONNECTION_ID,
            PROVIDER_CONNECTION_ACCOUNTS.ACCOUNT_ID,
            PROVIDER_CONNECTION_ACCOUNTS.EXTERNAL_ACCOUNT_ID,
            PROVIDER_CONNECTION_ACCOUNTS.EXTERNAL_METADATA,
        )
    }
}
