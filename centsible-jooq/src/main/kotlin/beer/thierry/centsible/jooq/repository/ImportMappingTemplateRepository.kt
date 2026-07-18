package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IImportMappingTemplateRepository
import beer.thierry.centsible.api.services.imports.CsvColumnMappingDTO
import beer.thierry.centsible.api.services.imports.CsvDialectDTO
import beer.thierry.centsible.api.services.imports.ImportMappingTemplateDTO
import beer.thierry.centsible.api.services.imports.ImportMappingTemplateForm
import beer.thierry.jooq.generated.tables.references.IMPORT_MAPPING_TEMPLATES
import tools.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class ImportMappingTemplateRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) : IImportMappingTemplateRepository {

    override fun fetchAll(authenticatedUser: UserDTO): List<ImportMappingTemplateDTO> =
        dsl.selectFrom(IMPORT_MAPPING_TEMPLATES)
            .where(IMPORT_MAPPING_TEMPLATES.USER_ID.eq(authenticatedUser.id))
            .orderBy(DSL.lower(IMPORT_MAPPING_TEMPLATES.NAME).asc())
            .fetch { mapToDTO(it) }

    override fun fetchById(authenticatedUser: UserDTO, id: UUID): ImportMappingTemplateDTO? =
        dsl.selectFrom(IMPORT_MAPPING_TEMPLATES)
            .where(IMPORT_MAPPING_TEMPLATES.USER_ID.eq(authenticatedUser.id).and(IMPORT_MAPPING_TEMPLATES.ID.eq(id)))
            .fetchOne { mapToDTO(it) }

    override fun existsByName(authenticatedUser: UserDTO, name: String, excludeId: UUID?): Boolean {
        var condition = IMPORT_MAPPING_TEMPLATES.USER_ID.eq(authenticatedUser.id)
            .and(DSL.lower(IMPORT_MAPPING_TEMPLATES.NAME).eq(name.trim().lowercase()))
        if (excludeId != null) {
            condition = condition.and(IMPORT_MAPPING_TEMPLATES.ID.ne(excludeId))
        }
        return (dsl.selectCount().from(IMPORT_MAPPING_TEMPLATES).where(condition).fetchOne(0, Int::class.java) ?: 0) > 0
    }

    override fun create(authenticatedUser: UserDTO, form: ImportMappingTemplateForm): ImportMappingTemplateDTO {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()
        dsl.insertInto(IMPORT_MAPPING_TEMPLATES)
            .set(IMPORT_MAPPING_TEMPLATES.ID, id)
            .set(IMPORT_MAPPING_TEMPLATES.USER_ID, authenticatedUser.id)
            .set(IMPORT_MAPPING_TEMPLATES.NAME, form.name.trim())
            .set(IMPORT_MAPPING_TEMPLATES.SOURCE_PROFILE_ID, form.sourceProfileId)
            .set(IMPORT_MAPPING_TEMPLATES.SOURCE_PROFILE_VERSION, form.sourceProfileVersion)
            .set(IMPORT_MAPPING_TEMPLATES.MAPPING, toJsonb(form.mapping))
            .set(IMPORT_MAPPING_TEMPLATES.DIALECT, toJsonb(form.dialect))
            .set(IMPORT_MAPPING_TEMPLATES.CREATED_AT, now)
            .set(IMPORT_MAPPING_TEMPLATES.MODIFIED_AT, now)
            .execute()
        return fetchById(authenticatedUser, id)
            ?: throw IllegalStateException("Created import mapping template could not be retrieved")
    }

    override fun update(
        authenticatedUser: UserDTO,
        id: UUID,
        form: ImportMappingTemplateForm,
    ): ImportMappingTemplateDTO? {
        val affected = dsl.update(IMPORT_MAPPING_TEMPLATES)
            .set(IMPORT_MAPPING_TEMPLATES.NAME, form.name.trim())
            .set(IMPORT_MAPPING_TEMPLATES.SOURCE_PROFILE_ID, form.sourceProfileId)
            .set(IMPORT_MAPPING_TEMPLATES.SOURCE_PROFILE_VERSION, form.sourceProfileVersion)
            .set(IMPORT_MAPPING_TEMPLATES.MAPPING, toJsonb(form.mapping))
            .set(IMPORT_MAPPING_TEMPLATES.DIALECT, toJsonb(form.dialect))
            .set(IMPORT_MAPPING_TEMPLATES.MODIFIED_AT, OffsetDateTime.now())
            .where(IMPORT_MAPPING_TEMPLATES.USER_ID.eq(authenticatedUser.id).and(IMPORT_MAPPING_TEMPLATES.ID.eq(id)))
            .execute()
        return if (affected > 0) fetchById(authenticatedUser, id) else null
    }

    override fun delete(authenticatedUser: UserDTO, id: UUID): Boolean =
        dsl.deleteFrom(IMPORT_MAPPING_TEMPLATES)
            .where(IMPORT_MAPPING_TEMPLATES.USER_ID.eq(authenticatedUser.id).and(IMPORT_MAPPING_TEMPLATES.ID.eq(id)))
            .execute() > 0

    private fun toJsonb(value: Any): JSONB = JSONB.valueOf(objectMapper.writeValueAsString(value))

    private fun mapToDTO(record: Record): ImportMappingTemplateDTO =
        ImportMappingTemplateDTO(
            id = record[IMPORT_MAPPING_TEMPLATES.ID],
            name = record[IMPORT_MAPPING_TEMPLATES.NAME] ?: "",
            sourceProfileId = record[IMPORT_MAPPING_TEMPLATES.SOURCE_PROFILE_ID],
            sourceProfileVersion = record[IMPORT_MAPPING_TEMPLATES.SOURCE_PROFILE_VERSION],
            mapping = readJsonb(record[IMPORT_MAPPING_TEMPLATES.MAPPING], CsvColumnMappingDTO::class.java) { CsvColumnMappingDTO() },
            dialect = readJsonb(record[IMPORT_MAPPING_TEMPLATES.DIALECT], CsvDialectDTO::class.java) { CsvDialectDTO() },
            createdAt = record[IMPORT_MAPPING_TEMPLATES.CREATED_AT],
            modifiedAt = record[IMPORT_MAPPING_TEMPLATES.MODIFIED_AT],
        )

    private fun <T> readJsonb(jsonb: JSONB?, type: Class<T>, default: () -> T): T {
        val raw = jsonb?.data() ?: return default()
        return try {
            objectMapper.readValue(raw, type)
        } catch (_: Exception) {
            default()
        }
    }
}
