package beer.thierry.centsible.api.services.imports

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class ParseHintsDTO(
    @field:Positive
    var defaultCategoryId: Long? = null,

    @field:Size(max = 3, min = 3)
    var targetCurrency: String? = null,

    var csvMapping: CsvColumnMappingDTO? = null,

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
