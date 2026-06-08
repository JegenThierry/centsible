package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.model.contact.ContactDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IContactsRepository
import beer.thierry.centsible.core.services.contacts.ContactService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ContactServiceTest {

    @Mock
    private lateinit var repository: IContactsRepository

    @InjectMocks
    private lateinit var service: ContactService

    private val user = UserDTO(UUID.randomUUID(), "u", "u@x", "U", "X", "U X", null)

    @Test
    fun `deleteContact refuses while outstanding loans exist`() {
        val id = UUID.randomUUID()
        `when`(repository.fetchContactById(user, id)).thenReturn(ContactDTO(id = id, firstName = "Alice"))
        `when`(repository.hasOutstandingLoans(id)).thenReturn(true)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.deleteContact(user, id)
        }
        assertTrue(ex.message!!.contains("outstanding"))
        verify(repository, never()).deleteContact(user, id)
    }

    @Test
    fun `deleteContact delegates to repository when there are no outstanding loans`() {
        val id = UUID.randomUUID()
        `when`(repository.fetchContactById(user, id)).thenReturn(ContactDTO(id = id, firstName = "Alice"))
        `when`(repository.hasOutstandingLoans(id)).thenReturn(false)
        `when`(repository.deleteContact(user, id)).thenReturn(true)

        assertEquals(true, service.deleteContact(user, id))
        verify(repository).deleteContact(user, id)
    }

    @Test
    fun `deleteContact never probes loan state for a contact the user does not own`() {
        val id = UUID.randomUUID()
        `when`(repository.fetchContactById(user, id)).thenReturn(null)

        assertEquals(false, service.deleteContact(user, id))
        verify(repository, never()).hasOutstandingLoans(id)
        verify(repository, never()).deleteContact(user, id)
    }

    @Test
    fun `fetchContactById delegates to repository`() {
        val id = UUID.randomUUID()
        val contact = ContactDTO(id = id, firstName = "Alice")
        `when`(repository.fetchContactById(user, id)).thenReturn(contact)

        assertEquals(contact, service.fetchContactById(user, id))
    }
}
