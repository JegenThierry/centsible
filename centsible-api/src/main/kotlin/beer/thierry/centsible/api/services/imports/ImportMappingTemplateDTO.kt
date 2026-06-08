package beer.thierry.centsible.api.services.imports

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

/**
 * A user-saved CSV import profile: a named, reusable [CsvColumnMappingDTO] + [CsvDialectDTO] so a
 * recurring statement from the same bank can be re-imported without re-walking the mapping wizard.
 * Persisted in the `import_mapping_templates` table with mapping/dialect stored as JSONB.
 */
data class ImportMappingTemplateDTO(
    var id: UUID? = null,
    var name: String = "",
    /** Set when the profile was derived from a built-in bank profile; enables auto-suggest on re-upload. */
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
