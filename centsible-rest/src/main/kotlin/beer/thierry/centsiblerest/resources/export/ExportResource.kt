package beer.thierry.centsiblerest.resources.export

import beer.thierry.centsible.api.model.export.ExportJobDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.account.IBudgetAccountService
import beer.thierry.centsible.api.services.export.IExportService
import beer.thierry.centsiblerest.resources.fileDownload
import beer.thierry.centsiblerest.resources.toDeleteResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
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
    private val budgetAccountService: IBudgetAccountService,
) {

    private val log = LoggerFactory.getLogger(ExportResource::class.java)

    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateExportRequest,
        @AuthenticationPrincipal user: UserDTO,
    ): ExportJobDTO {
        val params = requireNotNull(request.params) { "params is required" }
        val type = requireNotNull(request.type) { "type is required" }
        val title = requireNotNull(request.title) { "title is required" }
        val payload = protoBuilder.build(
            user = user,
            params = params,
            locale = user.locale,
            currency = primaryCurrencyOf(budgetAccountService.fetchAccounts(user).map { it.currency }),
            format = request.format,
        )
        val postProcessing = request.postProcessing.orEmpty().map {
            requireNotNull(it.type) { "post-processing type required" } to it.config.orEmpty()
        }
        val job = exportService.create(user, type, title, payload, postProcessing)
        log.info("Created export job id={} type={} userId={}", job.id, type, user.id)
        return job
    }

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "25") size: Int,
        @AuthenticationPrincipal user: UserDTO,
    ): List<ExportJobDTO> =
        exportService.list(user, page, size)

    @GetMapping("/{jobId}")
    fun get(
        @PathVariable jobId: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<ExportJobDTO> {
        val job = exportService.get(user, jobId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(job)
    }

    @GetMapping("/{jobId}/download")
    fun download(
        @PathVariable jobId: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<ByteArrayResource> {
        val pdf = exportService.download(user, jobId) ?: return ResponseEntity.notFound().build()
        return fileDownload(
            filename = pdf.filename,
            contentType = contentTypeFor(pdf.filename),
            length = pdf.bytes.size.toLong(),
            resource = ByteArrayResource(pdf.bytes),
            inline = false,
        )
    }

    private fun contentTypeFor(filename: String): MediaType =
        when (filename.substringAfterLast('.', "").lowercase()) {
            "pdf" -> MediaType.APPLICATION_PDF
            "csv" -> MediaType.parseMediaType("text/csv; charset=utf-8")
            "json" -> MediaType.APPLICATION_JSON
            else -> MediaType.APPLICATION_OCTET_STREAM
        }

    @PostMapping("/{jobId}/retrigger")
    fun retrigger(
        @PathVariable jobId: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<ExportJobDTO> {
        val job = exportService.retrigger(user, jobId) ?: return ResponseEntity.notFound().build()
        log.info("Retriggered export job id={} userId={}", jobId, user.id)
        return ResponseEntity.ok(job)
    }

    @DeleteMapping("/{jobId}")
    fun delete(
        @PathVariable jobId: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<Void> {
        val deleted = exportService.delete(user, jobId)
        if (deleted) log.info("Deleted export job id={} userId={}", jobId, user.id)
        return deleted.toDeleteResponse()
    }
}
