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
  const {t} = useNuxtApp().$i18n;
  const contactService = useContactService(api);

  const contacts = ref<Contact[]>([]);
  const pending = ref(false);

  const upsert = (contact: Contact) => upsertById(contacts, contact);

  async function updateContacts() {
    pending.value = true;
    try {
      contacts.value = await contactService.fetchContacts();
    } catch (error) {
      apiErrors.toastError(error, t('contacts.toasts.fetchFailedTitle'), t('contacts.toasts.fetchFailedBody'));
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
      apiErrors.toastError(error, t('contacts.toasts.loadDetailFailedTitle'), t('contacts.toasts.loadDetailFailedBody'));
      return undefined;
    }
  }

  async function createContact(form: ContactForm): Promise<Contact | undefined> {
    pending.value = true;
    try {
      const created = await contactService.createContact(form);
      upsert(created);
      toasts.success(t('contacts.toasts.createdTitle'), t('contacts.toasts.createdBody'));
      return created;
    } catch (error) {
      apiErrors.toastError(error, t('contacts.toasts.createFailedTitle'), t('contacts.toasts.genericErrorBody'));
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function updateContact(id: string, form: ContactForm) {
    pending.value = true;
    try {
      upsert(await contactService.updateContact(id, form));
      toasts.success(t('contacts.toasts.updatedTitle'), t('contacts.toasts.updatedBody'));
    } catch (error) {
      apiErrors.toastError(error, t('contacts.toasts.updateFailedTitle'), t('contacts.toasts.genericErrorBody'));
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function updateContactPicture(id: string, file: File) {
    pending.value = true;
    try {
      upsert(await contactService.updateContactPicture(id, file));
      toasts.success(t('contacts.toasts.pictureUpdatedTitle'), t('contacts.toasts.pictureUpdatedBody'));
    } catch (error) {
      apiErrors.toastError(error, t('contacts.toasts.pictureUpdateFailedTitle'), t('contacts.toasts.genericErrorBody'));
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function removeContactPicture(id: string) {
    pending.value = true;
    try {
      upsert(await contactService.removeContactPicture(id));
      toasts.success(t('contacts.toasts.pictureRemovedTitle'), t('contacts.toasts.pictureRemovedBody'));
    } catch (error) {
      apiErrors.toastError(error, t('contacts.toasts.pictureRemoveFailedTitle'), t('contacts.toasts.genericErrorBody'));
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
      toasts.success(t('contacts.toasts.deletedTitle'), t('contacts.toasts.deletedBody'));
    } catch (error) {
      apiErrors.toastError(error, t('contacts.toasts.deleteFailedTitle'), t('contacts.toasts.genericErrorBody'));
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
