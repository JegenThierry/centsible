package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.admin.AdminUserDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.admin.IAdminService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/admin")
@RestController
class AdminResource(private val adminService: IAdminService) {

    private val log = LoggerFactory.getLogger(AdminResource::class.java)

    @GetMapping("/users")
    fun listUsers(@AuthenticationPrincipal authenticatedUser: UserDTO): List<AdminUserDTO> =
        adminService.listUsers(authenticatedUser)

    @DeleteMapping("/users/{id}")
    fun deleteUser(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> {
        adminService.deleteUser(authenticatedUser, id)
        log.info("Admin deleted user id={} adminId={}", id, authenticatedUser.id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/users/{id}/resend-verification")
    fun resendVerification(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> {
        adminService.resendVerificationEmail(authenticatedUser, id)
        log.info("Admin resent verification email id={} adminId={}", id, authenticatedUser.id)
        return ResponseEntity.ok().build()
    }
}
