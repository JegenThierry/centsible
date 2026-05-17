package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.notification.NotificationDTO
import beer.thierry.centsible.api.model.notification.NotificationType
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.INotificationRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.*

@Repository
class NotificationRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) : INotificationRepository {

    private val table = DSL.table("notifications")
    private val idField = DSL.field("id", UUID::class.java)
    private val userIdField = DSL.field("user_id", UUID::class.java)
    private val typeField = DSL.field("type", String::class.java)
    private val titleField = DSL.field("title", String::class.java)
    private val bodyField = DSL.field("body", String::class.java)
    private val dataField = DSL.field("data", JSONB::class.java)
    private val readAtField = DSL.field("read_at", OffsetDateTime::class.java)
    private val createdAtField = DSL.field("created_at", OffsetDateTime::class.java)

    override fun create(
        user: UserDTO,
        type: NotificationType,
        title: String,
        body: String,
        data: Map<String, String>,
    ): NotificationDTO {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()
        val jsonValue = JSONB.valueOf(objectMapper.writeValueAsString(data))

        dsl.insertInto(table)
            .set(idField, id)
            .set(userIdField, user.id)
            .set(typeField, type.name)
            .set(titleField, title)
            .set(bodyField, body)
            .set(dataField, jsonValue)
            .set(createdAtField, now)
            .execute()

        return NotificationDTO(id, type, title, body, data, null, now)
    }

    override fun list(user: UserDTO, limit: Int): List<NotificationDTO> =
        dsl.select(idField, typeField, titleField, bodyField, dataField, readAtField, createdAtField)
            .from(table)
            .where(userIdField.eq(user.id))
            .orderBy(createdAtField.desc())
            .limit(limit.coerceIn(1, 200))
            .fetch { rec ->
                NotificationDTO(
                    id = rec[idField]!!,
                    type = NotificationType.valueOf(rec[typeField]!!),
                    title = rec[titleField]!!,
                    body = rec[bodyField]!!,
                    data = parseData(rec[dataField]),
                    readAt = rec[readAtField],
                    createdAt = rec[createdAtField]!!,
                )
            }

    override fun countUnread(user: UserDTO): Int =
        dsl.selectCount()
            .from(table)
            .where(userIdField.eq(user.id).and(readAtField.isNull))
            .fetchOne(0, Int::class.java) ?: 0

    override fun markRead(id: UUID, user: UserDTO): Boolean =
        dsl.update(table)
            .set(readAtField, OffsetDateTime.now())
            .where(idField.eq(id).and(userIdField.eq(user.id)).and(readAtField.isNull))
            .execute() > 0

    override fun markAllRead(user: UserDTO): Int =
        dsl.update(table)
            .set(readAtField, OffsetDateTime.now())
            .where(userIdField.eq(user.id).and(readAtField.isNull))
            .execute()

    override fun delete(id: UUID, user: UserDTO): Boolean =
        dsl.deleteFrom(table)
            .where(idField.eq(id).and(userIdField.eq(user.id)))
            .execute() > 0

    override fun hasRecent(user: UserDTO, type: NotificationType, budgetId: String, sincePeriodKey: String): Boolean {
        val periodField = DSL.field("data->>'periodKey'", String::class.java)
        val budgetField = DSL.field("data->>'budgetId'", String::class.java)
        return dsl.selectCount()
            .from(table)
            .where(
                userIdField.eq(user.id)
                    .and(typeField.eq(type.name))
                    .and(budgetField.eq(budgetId))
                    .and(periodField.eq(sincePeriodKey))
            )
            .fetchOne(0, Int::class.java) ?: 0 > 0
    }

    private fun parseData(jsonb: JSONB?): Map<String, String> {
        val raw = jsonb?.data() ?: return emptyMap()
        return try {
            @Suppress("UNCHECKED_CAST")
            (objectMapper.readValue(raw, Map::class.java) as Map<String, Any?>)
                .mapValues { (_, v) -> v?.toString() ?: "" }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
