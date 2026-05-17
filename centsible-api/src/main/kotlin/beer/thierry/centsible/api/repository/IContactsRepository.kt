package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.contact.ContactDTO
import beer.thierry.centsible.api.model.contact.ContactForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

interface IContactsRepository {
    fun fetchAllContacts(authenticatedUser: UserDTO): List<ContactDTO>
    fun fetchContactById(authenticatedUser: UserDTO, id: UUID): ContactDTO?
    fun createContact(authenticatedUser: UserDTO, form: ContactForm): ContactDTO
    fun updateContact(authenticatedUser: UserDTO, id: UUID, form: ContactForm): ContactDTO?
    fun updateContactPicture(authenticatedUser: UserDTO, id: UUID, picture: String?): ContactDTO?
    fun deleteContact(authenticatedUser: UserDTO, id: UUID): Boolean
    fun hasOutstandingLoans(contactId: UUID): Boolean
}
