package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.notification.NotificationDTO
import beer.thierry.centsible.api.model.notification.NotificationType
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.INotificationRepository
import beer.thierry.jooq.generated.tables.references.NOTIFICATIONS
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

        dsl.insertInto(NOTIFICATIONS)
            .set(NOTIFICATIONS.ID, id)
            .set(NOTIFICATIONS.USER_ID, user.id)
            .set(NOTIFICATIONS.TYPE, type.name)
            .set(NOTIFICATIONS.TITLE, title)
            .set(NOTIFICATIONS.BODY, body)
            .set(NOTIFICATIONS.DATA, jsonValue)
            .set(NOTIFICATIONS.CREATED_AT, now)
            .execute()

        return NotificationDTO(id, type, title, body, data, null, now)
    }

    override fun list(user: UserDTO, limit: Int): List<NotificationDTO> =
        dsl.selectFrom(NOTIFICATIONS)
            .where(NOTIFICATIONS.USER_ID.eq(user.id))
            .orderBy(NOTIFICATIONS.CREATED_AT.desc())
            .limit(limit.coerceIn(1, 200))
            .fetch { rec ->
                NotificationDTO(
                    id = rec.id!!,
                    type = NotificationType.valueOf(rec.type!!),
                    title = rec.title!!,
                    body = rec.body!!,
                    data = parseData(rec.data),
                    readAt = rec.readAt,
                    createdAt = rec.createdAt!!,
                )
            }

    override fun countUnread(user: UserDTO): Int =
        dsl.selectCount()
            .from(NOTIFICATIONS)
            .where(NOTIFICATIONS.USER_ID.eq(user.id).and(NOTIFICATIONS.READ_AT.isNull))
            .fetchOne(0, Int::class.java) ?: 0

    override fun markRead(id: UUID, user: UserDTO): Boolean =
        dsl.update(NOTIFICATIONS)
            .set(NOTIFICATIONS.READ_AT, OffsetDateTime.now())
            .where(NOTIFICATIONS.ID.eq(id).and(NOTIFICATIONS.USER_ID.eq(user.id)).and(NOTIFICATIONS.READ_AT.isNull))
            .execute() > 0

    override fun markAllRead(user: UserDTO): Int =
        dsl.update(NOTIFICATIONS)
            .set(NOTIFICATIONS.READ_AT, OffsetDateTime.now())
            .where(NOTIFICATIONS.USER_ID.eq(user.id).and(NOTIFICATIONS.READ_AT.isNull))
            .execute()

    override fun delete(id: UUID, user: UserDTO): Boolean =
        dsl.deleteFrom(NOTIFICATIONS)
            .where(NOTIFICATIONS.ID.eq(id).and(NOTIFICATIONS.USER_ID.eq(user.id)))
            .execute() > 0

    override fun hasRecent(user: UserDTO, type: NotificationType, budgetId: String, sincePeriodKey: String): Boolean {
        val periodField = DSL.field("data->>'periodKey'", String::class.java)
        val budgetField = DSL.field("data->>'budgetId'", String::class.java)
        val count = dsl.selectCount()
            .from(NOTIFICATIONS)
            .where(
                NOTIFICATIONS.USER_ID.eq(user.id)
                    .and(NOTIFICATIONS.TYPE.eq(type.name))
                    .and(budgetField.eq(budgetId))
                    .and(periodField.eq(sincePeriodKey))
            )
            .fetchOne(0, Int::class.java) ?: 0
        return count > 0
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
