package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.notification.NotificationDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.notifications.INotificationService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RequestMapping("/api/notifications")
@RestController
class NotificationsResource(private val service: INotificationService) {

    private val log = LoggerFactory.getLogger(NotificationsResource::class.java)

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "50") limit: Int,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<List<NotificationDTO>> = ResponseEntity.ok(service.list(user, limit))

    @GetMapping("/unread-count")
    fun unreadCount(@AuthenticationPrincipal user: UserDTO): ResponseEntity<Map<String, Int>> =
        ResponseEntity.ok(mapOf("count" to service.countUnread(user)))

    @PostMapping("/{id}/read")
    fun markRead(@PathVariable id: UUID, @AuthenticationPrincipal user: UserDTO): ResponseEntity<Void> {
        service.markRead(id, user)
        log.info("Marked notification read id={} userId={}", id, user.id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/read-all")
    fun markAllRead(@AuthenticationPrincipal user: UserDTO): ResponseEntity<Map<String, Int>> {
        val affected = service.markAllRead(user)
        log.info("Marked all notifications read userId={} affected={}", user.id, affected)
        return ResponseEntity.ok(mapOf("affected" to affected))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID, @AuthenticationPrincipal user: UserDTO): ResponseEntity<Void> {
        service.delete(id, user)
        log.info("Deleted notification id={} userId={}", id, user.id)
        return ResponseEntity.noContent().build()
    }
}
