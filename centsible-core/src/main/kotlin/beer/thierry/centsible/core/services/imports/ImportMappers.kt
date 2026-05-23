package beer.thierry.centsible.core.services.imports

import beer.thierry.centsible.api.services.imports.CsvColumnMappingDTO
import beer.thierry.centsible.api.services.imports.CsvDialectDTO
import beer.thierry.centsible.api.services.imports.ParseHintsDTO
import beer.thierry.centsible.imports.core.CsvColumnMapping
import beer.thierry.centsible.imports.core.CsvDialect
import beer.thierry.centsible.imports.core.ParseHints
import io.mcarle.konvert.api.Konverter

/**
 * Konvert generates `ImportMappersImpl` with field-by-field copies from the wire-format DTOs in
 * `centsible-api` to their `centsible-imports:core` counterparts. The core types add fields the
 * service derives later (e.g. ParseHints.locale from the user's profile) which Konvert leaves at
 * the data class default — explicit so a future field addition forces a decision instead of
 * silently propagating.
 */
@Konverter
interface ImportMappers {
    fun toCore(dto: ParseHintsDTO): ParseHints
    fun toCore(dto: CsvColumnMappingDTO): CsvColumnMapping
    fun toCore(dto: CsvDialectDTO): CsvDialect
}
