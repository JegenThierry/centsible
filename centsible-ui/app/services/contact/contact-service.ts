import type {AxiosInstance} from "axios";
import {assertStatus, postMultipart, validateRequest} from "~/composables/use-api";
import type {Contact, ContactForm} from "~/models/contact/contact";

export function useContactService(api: AxiosInstance) {
  async function fetchContacts(): Promise<Contact[]> {
    const response = await api.get<Contact[]>('/contacts');
    return validateRequest<Contact[]>(response);
  }

  async function fetchContact(id: string): Promise<Contact> {
    const response = await api.get<Contact>(`/contacts/${encodeURIComponent(id)}`);
    return validateRequest<Contact>(response);
  }

  async function createContact(form: ContactForm): Promise<Contact> {
    const response = await api.post<Contact>('/contacts', form);
    return validateRequest<Contact>(response);
  }

  async function updateContact(id: string, form: ContactForm): Promise<Contact> {
    const response = await api.put<Contact>(`/contacts/${encodeURIComponent(id)}`, form);
    return validateRequest<Contact>(response);
  }

  async function updateContactPicture(id: string, file: File): Promise<Contact> {
    return postMultipart<Contact>(api, `/contacts/${encodeURIComponent(id)}/picture`, {file});
  }

  async function removeContactPicture(id: string): Promise<Contact> {
    const response = await api.delete<Contact>(`/contacts/${encodeURIComponent(id)}/picture`);
    return validateRequest<Contact>(response);
  }

  async function deleteContact(id: string): Promise<void> {
    assertStatus(await api.delete(`/contacts/${encodeURIComponent(id)}`));
  }

  return {
    fetchContacts,
    fetchContact,
    createContact,
    updateContact,
    updateContactPicture,
    removeContactPicture,
    deleteContact,
  }
}
