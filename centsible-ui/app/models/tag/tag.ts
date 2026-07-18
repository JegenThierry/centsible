export interface Tag {
  id: number;
  name: string;
  color: string;
  createdAt?: string | null;
  modifiedAt?: string | null;
}

export type TagForm = Pick<Tag, 'name' | 'color'>;
