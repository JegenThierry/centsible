import type {AxiosInstance} from "axios";
import {crudResource, postMultipart, validateRequest} from "~/composables/use-api";
import type {Contact, ContactForm} from "~/models/contact/contact";

export function useContactService(api: AxiosInstance) {
  const resource = crudResource<Contact, string, ContactForm>(api, '/contacts');

  async function updateContactPicture(id: string, file: File): Promise<Contact> {
    return postMultipart<Contact>(api, `/contacts/${encodeURIComponent(id)}/picture`, {file});
  }

  async function removeContactPicture(id: string): Promise<Contact> {
    const response = await api.delete<Contact>(`/contacts/${encodeURIComponent(id)}/picture`);
    return validateRequest<Contact>(response);
  }

  return {
    fetchContacts: (): Promise<Contact[]> => resource.list(),
    fetchContact: (id: string): Promise<Contact> => resource.get(id),
    createContact: (form: ContactForm): Promise<Contact> => resource.create(form),
    updateContact: (id: string, form: ContactForm): Promise<Contact> => resource.update(id, form),
    updateContactPicture,
    removeContactPicture,
    deleteContact: (id: string): Promise<void> => resource.remove(id),
  }
}
