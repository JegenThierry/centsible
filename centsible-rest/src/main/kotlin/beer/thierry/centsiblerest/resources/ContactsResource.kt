package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.contact.ContactDTO
import beer.thierry.centsible.api.model.contact.ContactForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.contacts.IContactService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RequestMapping("/api/contacts")
@RestController
class ContactsResource(private val contactService: IContactService) {

    private val log = LoggerFactory.getLogger(ContactsResource::class.java)

    @GetMapping
    fun list(@AuthenticationPrincipal authenticatedUser: UserDTO): List<ContactDTO> =
        contactService.fetchAllContacts(authenticatedUser)

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<ContactDTO> {
        val contact = contactService.fetchContactById(authenticatedUser, id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(contact)
    }

    @PostMapping
    fun create(
        @Valid @RequestBody form: ContactForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ContactDTO {
        val created = contactService.createContact(authenticatedUser, form)
        log.info("Created contact id={} userId={}", created.id, authenticatedUser.id)
        return created
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody form: ContactForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<ContactDTO> {
        val updated = contactService.updateContact(authenticatedUser, id, form)
            ?: return ResponseEntity.notFound().build()
        log.info("Updated contact id={} userId={}", id, authenticatedUser.id)
        return ResponseEntity.ok(updated)
    }

    @PostMapping("/{id}/picture")
    fun updatePicture(
        @PathVariable id: UUID,
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<ContactDTO> {
        val updated = contactService.updateContactPicture(authenticatedUser, id, file.toValidatedImageDataUrl())
            ?: return ResponseEntity.notFound().build()
        log.info("Updated contact picture id={} userId={} sizeBytes={}", id, authenticatedUser.id, file.size)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}/picture")
    fun removePicture(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<ContactDTO> {
        val updated = contactService.updateContactPicture(authenticatedUser, id, null)
            ?: return ResponseEntity.notFound().build()
        log.info("Removed contact picture id={} userId={}", id, authenticatedUser.id)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> {
        val deleted = contactService.deleteContact(authenticatedUser, id)
        if (deleted) log.info("Deleted contact id={} userId={}", id, authenticatedUser.id)
        return deleted.toDeleteResponse()
    }
}
