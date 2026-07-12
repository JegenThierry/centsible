package beer.thierry.centsible.core.services.contacts

import beer.thierry.centsible.api.model.contact.ContactDTO
import beer.thierry.centsible.api.model.contact.ContactForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IContactsRepository
import beer.thierry.centsible.api.services.contacts.IContactService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ContactService(private val contactsRepository: IContactsRepository) : IContactService {

    private val log = LoggerFactory.getLogger(ContactService::class.java)

    override fun fetchAllContacts(authenticatedUser: UserDTO): List<ContactDTO> =
        contactsRepository.fetchAllContacts(authenticatedUser)

    override fun fetchContactById(authenticatedUser: UserDTO, id: UUID): ContactDTO? =
        contactsRepository.fetchContactById(authenticatedUser, id)

    override fun createContact(authenticatedUser: UserDTO, form: ContactForm): ContactDTO {
        val created = contactsRepository.createContact(authenticatedUser, form)
        log.info("Created contact id={} userId={}", created.id, authenticatedUser.id)
        return created
    }

    override fun updateContact(authenticatedUser: UserDTO, id: UUID, form: ContactForm): ContactDTO? {
        val updated = contactsRepository.updateContact(authenticatedUser, id, form)
        if (updated != null) {
            log.info("Updated contact id={} userId={}", id, authenticatedUser.id)
        }
        return updated
    }

    override fun updateContactPicture(authenticatedUser: UserDTO, id: UUID, picture: String?): ContactDTO? {
        val updated = contactsRepository.updateContactPicture(authenticatedUser, id, picture)
        if (updated != null) {
            log.info(
                "Updated contact picture id={} userId={} cleared={}",
                id, authenticatedUser.id, picture == null,
            )
        }
        return updated
    }

    override fun deleteContact(authenticatedUser: UserDTO, id: UUID): Boolean {
        contactsRepository.fetchContactById(authenticatedUser, id) ?: return false
        if (contactsRepository.hasOutstandingLoans(id)) {
            throw IllegalArgumentException("Cannot delete a contact with outstanding loans.")
        }
        val deleted = contactsRepository.deleteContact(authenticatedUser, id)
        if (deleted) {
            log.info("Deleted contact id={} userId={}", id, authenticatedUser.id)
        }
        return deleted
    }
}
