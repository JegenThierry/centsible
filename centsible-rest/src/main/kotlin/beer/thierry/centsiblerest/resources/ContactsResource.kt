package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.contact.ContactDTO
import beer.thierry.centsible.api.model.contact.ContactForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.contacts.IContactService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RequestMapping("/api/contacts")
@RestController
class ContactsResource(private val contactService: IContactService) {

    @GetMapping
    fun list(@AuthenticationPrincipal authenticatedUser: UserDTO): ResponseEntity<List<ContactDTO>> =
        ResponseEntity.ok(contactService.fetchAllContacts(authenticatedUser))

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
    ): ResponseEntity<ContactDTO> =
        ResponseEntity.ok(contactService.createContact(authenticatedUser, form))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody form: ContactForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<ContactDTO> {
        val updated = contactService.updateContact(authenticatedUser, id, form)
            ?: return ResponseEntity.notFound().build()
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
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}/picture")
    fun removePicture(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<ContactDTO> {
        val updated = contactService.updateContactPicture(authenticatedUser, id, null)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> =
        if (contactService.deleteContact(authenticatedUser, id)) ResponseEntity.ok().build()
        else ResponseEntity.notFound().build()
}
