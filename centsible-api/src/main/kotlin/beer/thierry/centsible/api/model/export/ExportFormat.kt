package beer.thierry.centsible.api.model.export

/**
 * Output format the export worker should render. Mirrors the `ExportFormat` enum in
 * centsible-proto/export.proto 1:1 — keep the two in sync. Add new entries at the end.
 */
enum class ExportFormat {
    PDF,
    CSV,
    JSON,
}
