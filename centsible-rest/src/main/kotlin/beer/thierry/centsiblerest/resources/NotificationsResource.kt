package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.notification.NotificationDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.notifications.INotificationService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RequestMapping("/api/notifications")
@RestController
class NotificationsResource(private val service: INotificationService) {

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
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/read-all")
    fun markAllRead(@AuthenticationPrincipal user: UserDTO): ResponseEntity<Map<String, Int>> =
        ResponseEntity.ok(mapOf("affected" to service.markAllRead(user)))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID, @AuthenticationPrincipal user: UserDTO): ResponseEntity<Void> {
        service.delete(id, user)
        return ResponseEntity.noContent().build()
    }
}
