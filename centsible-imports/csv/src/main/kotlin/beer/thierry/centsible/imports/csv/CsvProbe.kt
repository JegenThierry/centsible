package beer.thierry.centsible.imports.csv

import beer.thierry.centsible.imports.core.CsvDialect
import beer.thierry.centsible.imports.core.ParseWarning

data class CsvProbe(
    val header: List<String>,
    val sample: List<List<String>>,
    val detectedDialect: CsvDialect,
    val warnings: List<ParseWarning> = emptyList(),
)
