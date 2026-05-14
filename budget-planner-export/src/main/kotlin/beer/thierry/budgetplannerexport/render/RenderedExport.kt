package beer.thierry.budgetplannerexport.render

data class RenderedExport(
    val pdf: ByteArray,
    val filename: String,
)
