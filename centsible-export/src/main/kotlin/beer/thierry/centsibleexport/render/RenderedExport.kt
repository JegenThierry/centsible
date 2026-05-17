package beer.thierry.centsibleexport.render

data class RenderedExport(
    val pdf: ByteArray,
    val filename: String,
)
