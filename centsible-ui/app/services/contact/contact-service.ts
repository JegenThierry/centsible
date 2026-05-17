import type {AxiosInstance} from "axios";
import {validateRequest} from "~/composables/use-api";
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
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post<Contact>(
      `/contacts/${encodeURIComponent(id)}/picture`,
      formData,
      {headers: {'Content-Type': 'multipart/form-data'}}
    );
    return validateRequest<Contact>(response);
  }

  async function removeContactPicture(id: string): Promise<Contact> {
    const response = await api.delete<Contact>(`/contacts/${encodeURIComponent(id)}/picture`);
    return validateRequest<Contact>(response);
  }

  async function deleteContact(id: string): Promise<void> {
    const response = await api.delete(`/contacts/${encodeURIComponent(id)}`);
    if (response.status !== 200 && response.status !== 204) {
      throw new Error(response.statusText);
    }
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
