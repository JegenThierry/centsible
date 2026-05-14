package beer.thierry.budgetplanner.core.services.contacts

import beer.thierry.budgetplanner.api.model.contact.ContactDTO
import beer.thierry.budgetplanner.api.model.contact.ContactForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.repository.IContactsRepository
import beer.thierry.budgetplanner.api.services.contacts.IContactService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ContactService(private val contactsRepository: IContactsRepository) : IContactService {

    override fun fetchAllContacts(authenticatedUser: UserDTO): List<ContactDTO> =
        contactsRepository.fetchAllContacts(authenticatedUser)

    override fun fetchContactById(authenticatedUser: UserDTO, id: UUID): ContactDTO? =
        contactsRepository.fetchContactById(authenticatedUser, id)

    override fun createContact(authenticatedUser: UserDTO, form: ContactForm): ContactDTO =
        contactsRepository.createContact(authenticatedUser, form)

    override fun updateContact(authenticatedUser: UserDTO, id: UUID, form: ContactForm): ContactDTO? =
        contactsRepository.updateContact(authenticatedUser, id, form)

    override fun updateContactPicture(authenticatedUser: UserDTO, id: UUID, picture: String?): ContactDTO? =
        contactsRepository.updateContactPicture(authenticatedUser, id, picture)

    override fun deleteContact(authenticatedUser: UserDTO, id: UUID): Boolean {
        if (contactsRepository.hasOutstandingLoans(id)) {
            throw IllegalArgumentException("Cannot delete a contact with outstanding loans.")
        }
        return contactsRepository.deleteContact(authenticatedUser, id)
    }
}
