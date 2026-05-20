package beer.thierry.centsible.imports.csv

import beer.thierry.centsible.imports.core.CsvDialect

/**
 * CSV-specific inspection result. The REST layer calls [CsvFileParser.probe] right after format
 * detection to gather the header row and a small sample of data, so it can ask
 * [beer.thierry.centsible.imports.core.FileImportRegistry.bestProfileMatch] for a suggested
 * profile and surface that suggestion to the user.
 */
data class CsvProbe(
    val header: List<String>,
    val sample: List<List<String>>,
    val detectedDialect: CsvDialect,
)
