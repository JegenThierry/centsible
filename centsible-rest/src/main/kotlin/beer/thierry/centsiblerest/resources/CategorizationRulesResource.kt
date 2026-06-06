package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.categorization.CategorizationRuleDTO
import beer.thierry.centsible.api.model.categorization.CategorizationRuleForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.categorization.ICategorizationService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
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

@RequestMapping("/api/categorization-rules")
@RestController
class CategorizationRulesResource(private val categorizationService: ICategorizationService) {

    private val log = LoggerFactory.getLogger(CategorizationRulesResource::class.java)

    @GetMapping
    fun list(@AuthenticationPrincipal user: UserDTO): ResponseEntity<List<CategorizationRuleDTO>> =
        ResponseEntity.ok(categorizationService.list(user))

    @PostMapping
    fun create(
        @Valid @RequestBody form: CategorizationRuleForm,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<CategorizationRuleDTO> {
        val created = categorizationService.create(user, form)
        log.info("Created categorization rule id={} userId={}", created.id, user.id)
        return ResponseEntity.ok(created)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody form: CategorizationRuleForm,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<CategorizationRuleDTO> =
        ResponseEntity.ok(categorizationService.update(user, id, form))

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<Void> =
        if (categorizationService.delete(user, id)) {
            log.info("Deleted categorization rule id={} userId={}", id, user.id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }

    @PostMapping("/{id}/apply")
    fun apply(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<ApplyRuleResult> {
        val updated = categorizationService.applyToExisting(user, id)
        log.info("Applied categorization rule id={} to {} transaction(s) userId={}", id, updated, user.id)
        return ResponseEntity.ok(ApplyRuleResult(updated))
    }
}

data class ApplyRuleResult(val updated: Int)
