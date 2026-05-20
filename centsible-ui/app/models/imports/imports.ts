export interface ImportDetection {
  parserId: string;
  displayName: string;
  requiresMapping: boolean;
}

export interface CsvColumnMapping {
  dateColumn: number;
  descriptionColumn: number;
  amountColumn?: number | null;
  debitColumn?: number | null;
  creditColumn?: number | null;
  currencyColumn?: number | null;
  counterpartyColumn?: number | null;
  categoryColumn?: number | null;
  dateFormat: string;
  decimalSeparator: string;
  thousandsSeparator?: string | null;
  debitsArePositive: boolean;
}

export interface CsvDialect {
  delimiter: string;
  quote: string;
  hasHeader: boolean;
  encoding: string;
}

export interface CsvProbeResponse {
  header: string[];
  sample: string[][];
  dialect: CsvDialect;
  suggestedProfileId: string | null;
  suggestedProfileVersion: number | null;
  suggestedMapping: CsvColumnMapping | null;
}

export interface ParseHints {
  defaultCategoryId?: number | null;
  targetCurrency?: string | null;
  csvMapping?: CsvColumnMapping | null;
  csvDialect?: CsvDialect | null;
}

export interface ImportTransactionRow {
  amount: number;
  categoryId: number;
  description: string;
  transactionDate: string;
}

export interface ImportParseWarning {
  code: string;
  message: string;
  sourceRow?: number | null;
}

export interface ImportPreview {
  totalRows: number;
  sample: ImportTransactionRow[];
  warnings: ImportParseWarning[];
}

export interface ImportResult {
  imported: number;
  skippedDuplicates: number;
}

export interface CsvProfileSummary {
  id: string;
  displayName: string;
  localeTag: string | null;
  version: number;
}

export interface ParserSummary {
  id: string;
  displayName: string;
  requiresMapping: boolean;
  extensions: string[];
}
