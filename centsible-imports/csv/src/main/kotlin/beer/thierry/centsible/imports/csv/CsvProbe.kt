package beer.thierry.centsible.imports.csv

import beer.thierry.centsible.imports.core.CsvDialect
import beer.thierry.centsible.imports.core.ParseWarning

/**
 * CSV-specific inspection result. The REST layer calls [CsvFileParser.probe] right after format
 * detection to gather the header row and a small sample of data, so it can ask
 * [beer.thierry.centsible.imports.core.FileImportRegistry.bestProfileMatch] for a suggested
 * profile and surface that suggestion to the user.
 *
 * A file commons-csv cannot tokenize probes as empty [header] and [sample] with the reason in
 * [warnings], the same way [CsvFileParser.parse] reports it — an unreadable upload is a fact about
 * the file the wizard should show, not a failure of the probe.
 */
data class CsvProbe(
    val header: List<String>,
    val sample: List<List<String>>,
    val detectedDialect: CsvDialect,
    val warnings: List<ParseWarning> = emptyList(),
)
