package beer.thierry.centsiblerest.resources.export

import beer.thierry.centsible.api.model.export.ExportScheduleDTO
import beer.thierry.centsible.api.model.export.ExportScheduleForm
import beer.thierry.centsible.api.model.export.ExportScheduleUpdateForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.export.IExportScheduleService
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

@RequestMapping("/api/export-schedules")
@RestController
class ExportSchedulesResource(private val service: IExportScheduleService) {

    private val log = LoggerFactory.getLogger(ExportSchedulesResource::class.java)

    @GetMapping
    fun list(@AuthenticationPrincipal user: UserDTO): ResponseEntity<List<ExportScheduleDTO>> =
        ResponseEntity.ok(service.list(user))

    @PostMapping
    fun create(
        @Valid @RequestBody form: ExportScheduleForm,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<ExportScheduleDTO> {
        val created = service.create(user, form)
        log.info("Created export schedule id={} userId={}", created.id, user.id)
        return ResponseEntity.ok(created)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody form: ExportScheduleUpdateForm,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<ExportScheduleDTO> {
        val updated = service.update(user, id, form) ?: return ResponseEntity.notFound().build()
        log.info("Updated export schedule id={} userId={} active={}", id, user.id, form.active)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<Void> =
        if (service.delete(user, id)) {
            log.info("Deleted export schedule id={} userId={}", id, user.id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
}
