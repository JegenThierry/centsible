package beer.thierry.centsiblerest.resources

import org.springframework.http.ResponseEntity

/**
 * Standardizes the "delete or 404" idiom shared by the CRUD resources: a service returning `true`
 * means the row existed and was removed (204 No Content), `false` means it was never found (404).
 * Every delete endpoint answers with 204 on success so the API shape stays consistent.
 */
fun Boolean.toDeleteResponse(): ResponseEntity<Void> =
    if (this) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
