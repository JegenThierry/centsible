package beer.thierry.budgetplanner.jooq.repository

import beer.thierry.budgetplanner.api.model.export.ExportPostProcessingDTO
import beer.thierry.jooq.generated.tables.references.EXPORT_POST_PROCESSING
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.Record
import org.springframework.stereotype.Component

val POST_PROCESSING_COLUMNS = arrayOf(
    EXPORT_POST_PROCESSING.ID,
    EXPORT_POST_PROCESSING.EXPORT_JOB_ID,
    EXPORT_POST_PROCESSING.TYPE,
    EXPORT_POST_PROCESSING.STATUS,
    EXPORT_POST_PROCESSING.CONFIG,
    EXPORT_POST_PROCESSING.ERROR_MESSAGE,
    EXPORT_POST_PROCESSING.ATTEMPT_COUNT,
    EXPORT_POST_PROCESSING.COMPLETED_AT,
    EXPORT_POST_PROCESSING.CREATED_AT,
    EXPORT_POST_PROCESSING.MODIFIED_AT,
)

@Component
class ExportPostProcessingMapper(private val objectMapper: ObjectMapper) {

    private val mapTypeRef = object : TypeReference<Map<String, Any?>>() {}

    fun toDto(record: Record): ExportPostProcessingDTO {
        val configJson = record[EXPORT_POST_PROCESSING.CONFIG]?.data() ?: "{}"
        val config: Map<String, Any?> = objectMapper.readValue(configJson, mapTypeRef)
        return ExportPostProcessingDTO(
            id = record[EXPORT_POST_PROCESSING.ID]!!,
            exportJobId = record[EXPORT_POST_PROCESSING.EXPORT_JOB_ID]!!,
            type = record[EXPORT_POST_PROCESSING.TYPE]!!.toApi(),
            status = record[EXPORT_POST_PROCESSING.STATUS]!!.toApi(),
            config = config,
            errorMessage = record[EXPORT_POST_PROCESSING.ERROR_MESSAGE],
            attemptCount = record[EXPORT_POST_PROCESSING.ATTEMPT_COUNT]!!,
            completedAt = record[EXPORT_POST_PROCESSING.COMPLETED_AT],
            createdAt = record[EXPORT_POST_PROCESSING.CREATED_AT]!!,
            modifiedAt = record[EXPORT_POST_PROCESSING.MODIFIED_AT]!!,
        )
    }

    fun writeConfig(config: Map<String, Any?>): String = objectMapper.writeValueAsString(config)
}
