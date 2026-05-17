export type ImportColumnField = 'date' | 'amount' | 'description' | 'category' | 'ignore';

export const COLUMN_FIELD_LABELS: Record<ImportColumnField, string> = {
  date: 'Date',
  amount: 'Amount',
  description: 'Description',
  category: 'Category (optional)',
  ignore: 'Ignore',
};

export interface CsvPreviewRow {
  cells: string[];
}

export interface CsvParseResult {
  headers: string[];
  rows: CsvPreviewRow[];
}

export interface ColumnMapping {
  date: number | null;
  amount: number | null;
  description: number | null;
  category: number | null;
}

export interface ImportPayloadRow {
  amount: number;
  categoryId: number;
  description: string;
  transactionDate: string;
}

export interface ImportResult {
  imported: number;
  skippedDuplicates: number;
}
