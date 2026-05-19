export interface Attachment {
  id: string;
  transactionId: string;
  filename: string;
  contentType: string;
  sizeBytes: number;
  createdAt: string;
}

export interface EnrichedAttachment extends Attachment {
  accountId: string;
  transactionDescription: string | null;
  transactionDate: string;
}
