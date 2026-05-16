package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.contact.ContactDTO
import beer.thierry.centsible.api.model.contact.ContactForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IContactsRepository
import beer.thierry.jooq.generated.tables.references.CONTACTS
import beer.thierry.jooq.generated.tables.references.CONTACT_BALANCES
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class ContactsRepository(private val dsl: DSLContext) : IContactsRepository {

    override fun fetchAllContacts(authenticatedUser: UserDTO): List<ContactDTO> {
        return dsl.select(
            CONTACTS.ID,
            CONTACTS.FIRST_NAME,
            CONTACTS.LAST_NAME,
            CONTACTS.PICTURE,
            CONTACTS.CREATED_AT,
            CONTACT_BALANCES.TOTAL_LENT,
            CONTACT_BALANCES.TOTAL_OWED,
            CONTACT_BALANCES.TOTAL_REPAID,
            CONTACT_BALANCES.OUTSTANDING,
            CONTACT_BALANCES.OPEN_LOAN_COUNT,
            CONTACT_BALANCES.LAST_ACTIVITY_AT,
        )
            .from(CONTACTS)
            .leftJoin(CONTACT_BALANCES).on(CONTACT_BALANCES.CONTACT_ID.eq(CONTACTS.ID))
            .where(CONTACTS.USER_ID.eq(authenticatedUser.id))
            .orderBy(CONTACT_BALANCES.OUTSTANDING.desc().nullsLast(), CONTACTS.FIRST_NAME.asc())
            .fetch { mapToDTO(it) }
    }

    override fun fetchContactById(authenticatedUser: UserDTO, id: UUID): ContactDTO? {
        return dsl.select(
            CONTACTS.ID,
            CONTACTS.FIRST_NAME,
            CONTACTS.LAST_NAME,
            CONTACTS.PICTURE,
            CONTACTS.CREATED_AT,
            CONTACT_BALANCES.TOTAL_LENT,
            CONTACT_BALANCES.TOTAL_OWED,
            CONTACT_BALANCES.TOTAL_REPAID,
            CONTACT_BALANCES.OUTSTANDING,
            CONTACT_BALANCES.OPEN_LOAN_COUNT,
            CONTACT_BALANCES.LAST_ACTIVITY_AT,
        )
            .from(CONTACTS)
            .leftJoin(CONTACT_BALANCES).on(CONTACT_BALANCES.CONTACT_ID.eq(CONTACTS.ID))
            .where(CONTACTS.USER_ID.eq(authenticatedUser.id).and(CONTACTS.ID.eq(id)))
            .fetchOne { mapToDTO(it) }
    }

    override fun createContact(authenticatedUser: UserDTO, form: ContactForm): ContactDTO {
        val now = OffsetDateTime.now()
        val record = dsl.insertInto(CONTACTS)
            .set(CONTACTS.USER_ID, authenticatedUser.id)
            .set(CONTACTS.FIRST_NAME, form.firstName)
            .set(CONTACTS.LAST_NAME, form.lastName)
            .set(CONTACTS.CREATED_AT, now)
            .set(CONTACTS.MODIFIED_AT, now)
            .returning(CONTACTS.ID)
            .fetchOne() ?: throw IllegalStateException("Failed to create contact")

        return fetchContactById(authenticatedUser, record[CONTACTS.ID]!!)
            ?: throw IllegalStateException("Created contact could not be retrieved")
    }

    override fun updateContact(authenticatedUser: UserDTO, id: UUID, form: ContactForm): ContactDTO? {
        val affected = dsl.update(CONTACTS)
            .set(CONTACTS.FIRST_NAME, form.firstName)
            .set(CONTACTS.LAST_NAME, form.lastName)
            .set(CONTACTS.MODIFIED_AT, OffsetDateTime.now())
            .where(CONTACTS.USER_ID.eq(authenticatedUser.id).and(CONTACTS.ID.eq(id)))
            .execute()

        return if (affected > 0) fetchContactById(authenticatedUser, id) else null
    }

    override fun updateContactPicture(authenticatedUser: UserDTO, id: UUID, picture: String?): ContactDTO? {
        val affected = dsl.update(CONTACTS)
            .set(CONTACTS.PICTURE, picture)
            .set(CONTACTS.MODIFIED_AT, OffsetDateTime.now())
            .where(CONTACTS.USER_ID.eq(authenticatedUser.id).and(CONTACTS.ID.eq(id)))
            .execute()

        return if (affected > 0) fetchContactById(authenticatedUser, id) else null
    }

    override fun deleteContact(authenticatedUser: UserDTO, id: UUID): Boolean {
        return dsl.deleteFrom(CONTACTS)
            .where(CONTACTS.USER_ID.eq(authenticatedUser.id).and(CONTACTS.ID.eq(id)))
            .execute() > 0
    }

    override fun hasOutstandingLoans(contactId: UUID): Boolean {
        val outstanding = dsl.select(CONTACT_BALANCES.OUTSTANDING)
            .from(CONTACT_BALANCES)
            .where(CONTACT_BALANCES.CONTACT_ID.eq(contactId))
            .fetchOne()
            ?.get(CONTACT_BALANCES.OUTSTANDING)
            ?: BigDecimal.ZERO
        return outstanding > BigDecimal.ZERO
    }

    private fun mapToDTO(record: Record): ContactDTO {
        val first = record[CONTACTS.FIRST_NAME] ?: ""
        val last = record[CONTACTS.LAST_NAME]
        return ContactDTO(
            id = record[CONTACTS.ID],
            firstName = first,
            lastName = last,
            name = if (last.isNullOrBlank()) first else "$first $last",
            picture = record[CONTACTS.PICTURE],
            totalLent = record[CONTACT_BALANCES.TOTAL_LENT] ?: BigDecimal.ZERO,
            totalOwed = record[CONTACT_BALANCES.TOTAL_OWED] ?: BigDecimal.ZERO,
            totalRepaid = record[CONTACT_BALANCES.TOTAL_REPAID] ?: BigDecimal.ZERO,
            outstanding = record[CONTACT_BALANCES.OUTSTANDING] ?: BigDecimal.ZERO,
            openLoanCount = record[CONTACT_BALANCES.OPEN_LOAN_COUNT] ?: 0L,
            lastActivityAt = record[CONTACT_BALANCES.LAST_ACTIVITY_AT],
            createdAt = record[CONTACTS.CREATED_AT],
        )
    }
}
