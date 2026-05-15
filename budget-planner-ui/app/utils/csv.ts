import type {CsvParseResult, CsvPreviewRow} from "~/models/transactions/csv-import";

/**
 * Parse a small CSV string into headers + rows. Handles quoted fields and escaped quotes.
 * Not RFC 4180 strict; sufficient for typical bank exports.
 */
export function parseCsv(text: string, delimiter: string = ','): CsvParseResult {
  const lines: string[][] = [];
  let current: string[] = [];
  let buffer = '';
  let inQuotes = false;

  for (let i = 0; i < text.length; i++) {
    const ch = text[i];

    if (inQuotes) {
      if (ch === '"' && text[i + 1] === '"') {
        buffer += '"';
        i++;
      } else if (ch === '"') {
        inQuotes = false;
      } else {
        buffer += ch;
      }
      continue;
    }

    if (ch === '"') {
      inQuotes = true;
    } else if (ch === delimiter) {
      current.push(buffer);
      buffer = '';
    } else if (ch === '\n' || ch === '\r') {
      if (buffer.length > 0 || current.length > 0) {
        current.push(buffer);
        lines.push(current);
        current = [];
        buffer = '';
      }
      if (ch === '\r' && text[i + 1] === '\n') i++;
    } else {
      buffer += ch;
    }
  }
  if (buffer.length > 0 || current.length > 0) {
    current.push(buffer);
    lines.push(current);
  }

  if (lines.length === 0) return {headers: [], rows: []};
  const headers = (lines[0] ?? []).map(h => h.trim());
  const rows: CsvPreviewRow[] = lines.slice(1).map(cells => ({cells}));
  return {headers, rows};
}

/**
 * Parse an amount like "1,234.56", "1.234,56", "-12.5", "(15.00)" into a positive number.
 * Returns NaN if it can't be parsed.
 */
export function parseAmount(raw: string): number {
  if (!raw) return NaN;
  let s = raw.trim();
  const isNegative = s.startsWith('-') || (s.startsWith('(') && s.endsWith(')'));
  s = s.replace(/[()-]/g, '').replace(/[€$£¥]/g, '').trim();

  const lastComma = s.lastIndexOf(',');
  const lastDot = s.lastIndexOf('.');
  if (lastComma > lastDot) {
    // European format: 1.234,56 → 1234.56
    s = s.replace(/\./g, '').replace(',', '.');
  } else {
    s = s.replace(/,/g, '');
  }
  const n = Number(s);
  if (Number.isNaN(n)) return NaN;
  return isNegative ? -Math.abs(n) : n;
}

export type DateFormat = 'auto' | 'dd/MM/yyyy' | 'MM/dd/yyyy' | 'yyyy-MM-dd';

/**
 * Normalize a date string to ISO `yyyy-MM-dd`. Returns null if it can't be parsed.
 *
 * `format` controls how ambiguous formats are interpreted:
 * - `'yyyy-MM-dd'`: only ISO; rejects anything else
 * - `'dd/MM/yyyy'`: day-first (European), accepts `/`, `.`, or `-` separators
 * - `'MM/dd/yyyy'`: month-first (US), accepts `/`, `.`, or `-` separators
 * - `'auto'` (default): tries ISO first, then day-first, then month-first.
 *   Caller should prefer an explicit format to avoid silently swapping day/month.
 */
export function parseToIsoDate(raw: string, format: DateFormat = 'auto'): string | null {
  if (!raw) return null;
  const s = raw.trim();
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) return s.slice(0, 10);
  if (format === 'yyyy-MM-dd') return null;

  const parts = s.split(/[/.-]/);
  if (parts.length !== 3) return null;
  const [a, b, c] = parts;
  if (!a || !b || !c || c.length !== 4) return null;

  const first = Number(a), second = Number(b), year = c;
  const tryDayFirst = () => buildIso(year, second, first);
  const tryMonthFirst = () => buildIso(year, first, second);

  if (format === 'dd/MM/yyyy') return tryDayFirst();
  if (format === 'MM/dd/yyyy') return tryMonthFirst();
  return tryDayFirst() ?? tryMonthFirst();
}

function buildIso(year: string, month: number, day: number): string | null {
  if (!Number.isInteger(month) || !Number.isInteger(day)) return null;
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

