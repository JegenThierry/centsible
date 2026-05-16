package beer.thierry.centsiblerest.resources.export

import beer.thierry.centsible.api.model.export.ExportJobDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.export.IExportService
import jakarta.validation.Valid
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/exports")
@RestController
class ExportResource(
    private val exportService: IExportService,
    private val protoBuilder: ExportProtoBuilder,
) {

    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateExportRequest,
        @AuthenticationPrincipal user: UserDTO?,
    ): ResponseEntity<ExportJobDTO> {
        if (user == null) return ResponseEntity.status(401).build()
        val params = request.params ?: throw IllegalArgumentException("params is required")
        val type = request.type ?: throw IllegalArgumentException("type is required")
        val title = request.title ?: throw IllegalArgumentException("title is required")
        val payload = protoBuilder.build(user = user, params = params, locale = "en", currency = "EUR")
        val postProcessing = request.postProcessing.orEmpty().map {
            (it.type ?: throw IllegalArgumentException("post-processing type required")) to (it.config.orEmpty())
        }
        val job = exportService.create(user, type, title, payload, postProcessing)
        return ResponseEntity.ok(job)
    }

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "25") size: Int,
        @AuthenticationPrincipal user: UserDTO?,
    ): ResponseEntity<List<ExportJobDTO>> {
        if (user == null) return ResponseEntity.status(401).build()
        return ResponseEntity.ok(exportService.list(user, page, size))
    }

    @GetMapping("/{jobId}")
    fun get(
        @PathVariable jobId: UUID,
        @AuthenticationPrincipal user: UserDTO?,
    ): ResponseEntity<ExportJobDTO> {
        if (user == null) return ResponseEntity.status(401).build()
        val job = exportService.get(user, jobId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(job)
    }

    @GetMapping("/{jobId}/download")
    fun download(
        @PathVariable jobId: UUID,
        @AuthenticationPrincipal user: UserDTO?,
    ): ResponseEntity<ByteArrayResource> {
        if (user == null) return ResponseEntity.status(401).build()
        val pdf = exportService.download(user, jobId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${pdf.filename}\"")
            .contentLength(pdf.bytes.size.toLong())
            .body(ByteArrayResource(pdf.bytes))
    }

    @PostMapping("/{jobId}/retrigger")
    fun retrigger(
        @PathVariable jobId: UUID,
        @AuthenticationPrincipal user: UserDTO?,
    ): ResponseEntity<ExportJobDTO> {
        if (user == null) return ResponseEntity.status(401).build()
        val job = exportService.retrigger(user, jobId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(job)
    }

    @DeleteMapping("/{jobId}")
    fun delete(
        @PathVariable jobId: UUID,
        @AuthenticationPrincipal user: UserDTO?,
    ): ResponseEntity<Void> {
        if (user == null) return ResponseEntity.status(401).build()
        return if (exportService.delete(user, jobId)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
    }
}
