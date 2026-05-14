export interface Contact {
  id: string;
  firstName: string;
  lastName?: string;
  name: string;
  picture?: string;
  totalLent: number;
  totalOwed: number;
  totalRepaid: number;
  outstanding: number;
  openLoanCount: number;
  lastActivityAt?: string;
  createdAt?: string;
}

export interface ContactForm {
  firstName: string;
  lastName?: string;
}
