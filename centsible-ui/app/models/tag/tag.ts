export interface Tag {
  id: number;
  name: string;
  color: string;
  createdAt?: string | null;
  modifiedAt?: string | null;
}

export interface TagForm {
  name: string;
  color: string;
}
