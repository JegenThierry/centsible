package beer.thierry.centsible.api.services.imports

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/**
 * Transport-friendly subset of [beer.thierry.centsible.imports.core.ParseHints]. Lives in
 * centsible-api so REST controllers and the service interface can speak the same vocabulary
 * without depending on centsible-imports:core. The core ParseHints adds locale/Charset/etc.
 * that the service constructs internally from this DTO + the authenticated user's profile.
 */
data class ParseHintsDTO(
    @field:Positive
    var defaultCategoryId: Long? = null,

    /** ISO 4217 currency of the target account. Helps parsers normalize signs. */
    @field:Size(max = 3, min = 3)
    var targetCurrency: String? = null,

    /** CSV mapping selected in the wizard. Required for CSV; ignored by other formats. */
    var csvMapping: CsvColumnMappingDTO? = null,

    /** CSV dialect overrides. Ignored by other formats. */
    var csvDialect: CsvDialectDTO? = null,
)

data class CsvColumnMappingDTO(
    var dateColumn: Int = 0,
    var descriptionColumn: Int = 1,
    var amountColumn: Int? = null,
    var debitColumn: Int? = null,
    var creditColumn: Int? = null,
    var currencyColumn: Int? = null,
    var counterpartyColumn: Int? = null,
    var categoryColumn: Int? = null,
    var dateFormat: String = "yyyy-MM-dd",
    var decimalSeparator: Char = '.',
    var thousandsSeparator: Char? = null,
    var debitsArePositive: Boolean = true,
)

data class CsvDialectDTO(
    var delimiter: Char = ',',
    var quote: Char = '"',
    var hasHeader: Boolean = true,
    var encoding: String = "UTF-8",
)
