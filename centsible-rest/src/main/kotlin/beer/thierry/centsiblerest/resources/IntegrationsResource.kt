package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.integrations.ProviderConnectionDTO
import beer.thierry.centsible.api.model.integrations.ProviderConnectionForm
import beer.thierry.centsible.api.model.integrations.ProviderDescriptor
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.integrations.IProviderConnectionService
import beer.thierry.centsible.api.services.integrations.IProviderRegistry
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/integrations")
@RestController
class IntegrationsResource(
    private val registry: IProviderRegistry,
    private val connectionService: IProviderConnectionService,
) {

    @GetMapping("/providers")
    fun listProviders(
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<List<ProviderDescriptor>> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        return ResponseEntity.ok(registry.listDescriptors())
    }

    @GetMapping("/connections")
    fun listConnections(
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<List<ProviderConnectionDTO>> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        return ResponseEntity.ok(connectionService.fetchAllConnections(authenticatedUser))
    }

    @GetMapping("/connections/{id}")
    fun getConnection(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<ProviderConnectionDTO> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        val conn = connectionService.fetchConnectionById(authenticatedUser, id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(conn)
    }

    @PostMapping("/connections")
    fun createConnection(
        @Valid @RequestBody form: ProviderConnectionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<ProviderConnectionDTO> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        return ResponseEntity.ok(connectionService.createConnection(authenticatedUser, form))
    }

    @PutMapping("/connections/{id}")
    fun updateConnection(
        @PathVariable id: UUID,
        @Valid @RequestBody form: ProviderConnectionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<ProviderConnectionDTO> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        val updated = connectionService.updateConnection(authenticatedUser, id, form)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/connections/{id}")
    fun deleteConnection(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<Void> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        return if (connectionService.deleteConnection(authenticatedUser, id)) ResponseEntity.ok().build()
        else ResponseEntity.notFound().build()
    }

    @PostMapping("/connections/{id}/sync")
    fun triggerSync(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<Void> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        return if (connectionService.triggerSync(authenticatedUser, id)) ResponseEntity.accepted().build()
        else ResponseEntity.notFound().build()
    }

    /** OAuth2 callback. Stub — permit-all because identity comes from `state`, not a JWT. */
    @GetMapping("/oauth/callback/{providerKey}")
    fun oauthCallback(@PathVariable providerKey: String): ResponseEntity<String> {
        val descriptor = registry.getDescriptor(providerKey)
            ?: return ResponseEntity.status(404).body("Unknown provider: $providerKey")
        return ResponseEntity.status(501).body(
            "OAuth callback for '${descriptor.key}' is not yet implemented on this server."
        )
    }
}
