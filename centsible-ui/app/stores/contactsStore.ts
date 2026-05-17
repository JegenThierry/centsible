import {defineStore} from "pinia";
import type {Contact, ContactForm} from "~/models/contact/contact";
import {useContactService} from "~/services/contact/contact-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {upsertById} from "~/utils/upsert";

export const useContactsStore = defineStore('contactsStore', () => {
  const api = useApi();
  const toasts = useToasts();
  const apiErrors = useApiErrors();
  const contactService = useContactService(api);

  const contacts = ref<Contact[]>([]);
  const pending = ref(false);

  const upsert = (contact: Contact) => upsertById(contacts, contact);

  async function updateContacts() {
    pending.value = true;
    try {
      contacts.value = await contactService.fetchContacts();
    } catch (error) {
      apiErrors.toastError(error, "Failed to fetch contacts", "Contacts could not be loaded");
    } finally {
      pending.value = false;
    }
  }

  async function fetchContact(id: string): Promise<Contact | undefined> {
    try {
      const contact = await contactService.fetchContact(id);
      upsert(contact);
      return contact;
    } catch (error) {
      apiErrors.toastError(error, "Failed to load contact", "Contact could not be loaded");
      return undefined;
    }
  }

  async function createContact(form: ContactForm): Promise<Contact | undefined> {
    pending.value = true;
    try {
      const created = await contactService.createContact(form);
      upsert(created);
      toasts.success("Contact created", "New contact has been added");
      return created;
    } catch (error) {
      apiErrors.toastError(error, "Failed to create contact", "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function updateContact(id: string, form: ContactForm) {
    pending.value = true;
    try {
      upsert(await contactService.updateContact(id, form));
      toasts.success("Contact updated", "Contact has been updated");
    } catch (error) {
      apiErrors.toastError(error, "Failed to update contact", "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function updateContactPicture(id: string, file: File) {
    pending.value = true;
    try {
      upsert(await contactService.updateContactPicture(id, file));
      toasts.success("Picture updated", "Contact picture has been updated");
    } catch (error) {
      apiErrors.toastError(error, "Failed to update picture", "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function removeContactPicture(id: string) {
    pending.value = true;
    try {
      upsert(await contactService.removeContactPicture(id));
      toasts.success("Picture removed", "Contact picture has been removed");
    } catch (error) {
      apiErrors.toastError(error, "Failed to remove picture", "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function deleteContact(id: string) {
    pending.value = true;
    try {
      await contactService.deleteContact(id);
      contacts.value = contacts.value.filter(c => c.id !== id);
      toasts.success("Contact deleted", "Contact has been removed");
    } catch (error) {
      apiErrors.toastError(error, "Failed to delete contact", "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  function findContactById(id: string): Contact | undefined {
    return contacts.value.find(c => c.id === id);
  }

  return {
    contacts,
    pending,
    updateContacts,
    fetchContact,
    createContact,
    updateContact,
    updateContactPicture,
    removeContactPicture,
    deleteContact,
    findContactById,
  }
});
