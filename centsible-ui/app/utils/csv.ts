import Papa from 'papaparse';
import type {CsvParseResult, CsvPreviewRow} from "~/models/transactions/csv-import";

export function parseCsv(text: string, delimiter: string = ','): CsvParseResult {
  const {data} = Papa.parse<string[]>(text, {delimiter, skipEmptyLines: true});
  const [headerRow, ...dataRows] = data;
  if (!headerRow) return {headers: [], rows: []};
  const headers = headerRow.map(h => h.trim());
  const rows: CsvPreviewRow[] = dataRows.map(cells => ({cells}));
  return {headers, rows};
}

export function parseAmount(raw: string): number {
  if (!raw) return NaN;
  let s = raw.trim();
  const isNegative = s.startsWith('-') || (s.startsWith('(') && s.endsWith(')'));
  s = s.replace(/[()-]/g, '').replace(/[€$£¥]/g, '').trim();

  const lastComma = s.lastIndexOf(',');
  const lastDot = s.lastIndexOf('.');
  if (lastComma > lastDot) {
    s = s.replace(/\./g, '').replace(',', '.');
  } else {
    s = s.replace(/,/g, '');
  }
  const n = Number(s);
  if (Number.isNaN(n)) return NaN;
  return isNegative ? -Math.abs(n) : n;
}

export type DateFormat = 'auto' | 'dd/MM/yyyy' | 'MM/dd/yyyy' | 'yyyy-MM-dd';

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

