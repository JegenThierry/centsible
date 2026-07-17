package beer.thierry.centsible.api.services.imports

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

data class ImportMappingTemplateDTO(
    var id: UUID? = null,
    var name: String = "",
    var sourceProfileId: String? = null,
    var sourceProfileVersion: Int? = null,
    var mapping: CsvColumnMappingDTO = CsvColumnMappingDTO(),
    var dialect: CsvDialectDTO = CsvDialectDTO(),
    var createdAt: OffsetDateTime? = null,
    var modifiedAt: OffsetDateTime? = null,
)

data class ImportMappingTemplateForm(
    @field:NotBlank(message = "{validation.importTemplate.name.required}")
    @field:Size(max = 100, message = "{validation.importTemplate.name.tooLong}")
    var name: String = "",

    var sourceProfileId: String? = null,

    var sourceProfileVersion: Int? = null,

    var mapping: CsvColumnMappingDTO = CsvColumnMappingDTO(),

    var dialect: CsvDialectDTO = CsvDialectDTO(),
)
