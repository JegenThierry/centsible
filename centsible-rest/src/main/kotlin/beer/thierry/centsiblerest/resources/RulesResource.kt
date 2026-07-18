package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.rule.RuleDTO
import beer.thierry.centsible.api.model.rule.RuleForm
import beer.thierry.centsible.api.model.rule.RulePreviewResult
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.rule.IRuleService
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

@RequestMapping("/api/rules")
@RestController
class RulesResource(private val ruleService: IRuleService) {

    private val log = LoggerFactory.getLogger(RulesResource::class.java)

    @GetMapping
    fun list(@AuthenticationPrincipal user: UserDTO): List<RuleDTO> =
        ruleService.list(user)

    @PostMapping
    fun create(
        @Valid @RequestBody form: RuleForm,
        @AuthenticationPrincipal user: UserDTO,
    ): RuleDTO {
        val created = ruleService.create(user, form)
        log.info("Created rule id={} userId={}", created.id, user.id)
        return created
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody form: RuleForm,
        @AuthenticationPrincipal user: UserDTO,
    ): RuleDTO =
        ruleService.update(user, id, form)

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<Void> {
        val deleted = ruleService.delete(user, id)
        if (deleted) log.info("Deleted rule id={} userId={}", id, user.id)
        return deleted.toDeleteResponse()
    }

    @PostMapping("/preview")
    fun preview(
        @RequestBody form: RuleForm,
        @AuthenticationPrincipal user: UserDTO,
    ): RulePreviewResult =
        ruleService.preview(user, form)

    @PostMapping("/{id}/apply")
    fun apply(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ApplyRuleResult {
        val updated = ruleService.applyToExisting(user, id)
        log.info("Applied rule id={} to {} transaction(s) userId={}", id, updated, user.id)
        return ApplyRuleResult(updated)
    }
}

data class ApplyRuleResult(val updated: Int)
