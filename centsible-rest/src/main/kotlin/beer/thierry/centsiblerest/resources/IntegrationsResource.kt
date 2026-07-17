package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.integrations.ProviderConnectionDTO
import beer.thierry.centsible.api.model.integrations.ProviderConnectionForm
import beer.thierry.centsible.api.model.integrations.ProviderDescriptor
import beer.thierry.centsible.api.model.integrations.SelectOption
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.integrations.IOAuthFlowService
import beer.thierry.centsible.api.services.integrations.IProviderConnectionService
import beer.thierry.centsible.api.services.integrations.IProviderRegistry
import beer.thierry.centsible.api.services.integrations.OAuthCompletionResult
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Where to redirect the user's browser after the OAuth callback finishes. In a split deployment
 * the UI is on a different host from the API; defaults to integrations.base-url for single-host
 * setups.
 */
private const val UI_BASE_URL_EXPRESSION = "\${integrations.ui-base-url:\${integrations.base-url:}}"

@RequestMapping("/api/integrations")
@RestController
class IntegrationsResource(
    private val registry: IProviderRegistry,
    private val connectionService: IProviderConnectionService,
    private val oauthFlowService: IOAuthFlowService,
    @Value(UI_BASE_URL_EXPRESSION) private val uiBaseUrl: String,
) {

    private val log = LoggerFactory.getLogger(IntegrationsResource::class.java)

    @GetMapping("/providers")
    fun listProviders(): List<ProviderDescriptor> =
        registry.listDescriptors()

    @GetMapping("/connections")
    fun listConnections(
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<ProviderConnectionDTO> =
        connectionService.fetchAllConnections(authenticatedUser)

    @GetMapping("/connections/{id}")
    fun getConnection(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<ProviderConnectionDTO> {
        val conn = connectionService.fetchConnectionById(authenticatedUser, id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(conn)
    }

    @PostMapping("/connections")
    fun createConnection(
        @Valid @RequestBody form: ProviderConnectionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ProviderConnectionDTO {
        val created = connectionService.createConnection(authenticatedUser, form)
        log.info("Created provider connection id={} providerKey={} userId={}", created.id, form.providerKey, authenticatedUser.id)
        return created
    }

    @PutMapping("/connections/{id}")
    fun updateConnection(
        @PathVariable id: UUID,
        @Valid @RequestBody form: ProviderConnectionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<ProviderConnectionDTO> {
        val updated = connectionService.updateConnection(authenticatedUser, id, form)
            ?: return ResponseEntity.notFound().build()
        log.info("Updated provider connection id={} userId={}", id, authenticatedUser.id)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/connections/{id}")
    fun deleteConnection(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> {
        val deleted = connectionService.deleteConnection(authenticatedUser, id)
        if (deleted) log.info("Deleted provider connection id={} userId={}", id, authenticatedUser.id)
        return deleted.toDeleteResponse()
    }

    @PostMapping("/connections/{id}/sync")
    fun triggerSync(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> =
        if (connectionService.triggerSync(authenticatedUser, id)) {
            log.info("Triggered sync for connection id={} userId={}", id, authenticatedUser.id)
            ResponseEntity.accepted().build()
        } else ResponseEntity.notFound().build()

    /**
     * Begins an OAuth2 authorization-code flow. Returns the URL the browser should follow to
     * the bank/provider's consent screen.
     */
    @PostMapping("/connections/{id}/oauth/start")
    fun startOAuth(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): OAuthStartResponse {
        val result = oauthFlowService.startAuthorization(authenticatedUser, id)
        log.info("Started OAuth authorization connectionId={} userId={}", result.connectionId, authenticatedUser.id)
        return OAuthStartResponse(authorizationUrl = result.authorizationUrl, connectionId = result.connectionId)
    }

    /**
     * Resolves dropdown options for a SELECT_REMOTE field on the given provider.
     * Body: { "query": "optional search", "values": { other form values } }
     */
    @PostMapping("/providers/{providerKey}/options/{fieldName}")
    fun remoteOptions(
        @PathVariable providerKey: String,
        @PathVariable fieldName: String,
        @RequestBody(required = false) request: RemoteOptionsRequestBody?,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<SelectOption> {
        val req = request ?: RemoteOptionsRequestBody()
        return connectionService.fetchRemoteOptions(
            authenticatedUser = authenticatedUser,
            providerKey = providerKey,
            fieldName = fieldName,
            query = req.query,
            values = req.values,
        )
    }

    /**
     * OAuth2 callback. Public-by-necessity (browser arrives without a JWT after the redirect
     * from the provider). Authentication is via an opaque single-use `state` token issued by
     * the OAuthStateStore and verified inside OAuthFlowService. We respond with an HTTP 302 to
     * a static frontend route so the user lands somewhere sensible regardless of success/failure.
     */
    @GetMapping("/oauth/callback/{providerKey}")
    fun oauthCallback(
        @PathVariable providerKey: String,
        request: HttpServletRequest,
    ): RedirectView {
        val state = request.getParameter("state")
            ?: request.getParameter("ref")
            ?: ""
        val params = request.parameterMap.mapValues { it.value.firstOrNull().orEmpty() }
        val result = oauthFlowService.completeAuthorization(providerKey, state, params)
        when (result) {
            is OAuthCompletionResult.Success ->
                log.info("OAuth callback success providerKey={} connectionId={}", providerKey, result.connectionId)
            is OAuthCompletionResult.Failure ->
                log.warn(
                    "OAuth callback failure providerKey={} connectionId={} reason={}",
                    providerKey, result.connectionId, result.reason,
                )
        }
        val target = buildReturnUrl(result)
        return RedirectView(target, false, false, false)
    }

    private fun buildReturnUrl(result: OAuthCompletionResult): String {
        val front = uiBaseUrl.trim().removeSuffix("/").ifEmpty { "" }
        val (status, connectionId, errorCode) = when (result) {
            is OAuthCompletionResult.Success -> Triple("ok", result.connectionId.toString(), null)
            is OAuthCompletionResult.Failure -> Triple("error", result.connectionId?.toString(), result.reason)
        }
        val params = buildString {
            append("status=$status")
            connectionId?.let { append("&connection=").append(URLEncoder.encode(it, StandardCharsets.UTF_8)) }
            errorCode?.let { append("&code=").append(URLEncoder.encode(it, StandardCharsets.UTF_8)) }
        }
        return "$front/integrations/oauth-return?$params"
    }
}

data class OAuthStartResponse(
    val authorizationUrl: String,
    val connectionId: UUID,
)

data class RemoteOptionsRequestBody(
    val query: String? = null,
    val values: Map<String, Any?> = emptyMap(),
)
