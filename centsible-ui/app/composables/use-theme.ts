export type ThemeMode = 'light' | 'dark' | 'system';
export type ThemeId = 'lavender' | 'mint' | 'peach' | 'blush' | 'ocean' | 'nuxt';
type Palette = 'lavender' | 'mint' | 'peach' | 'blush' | 'ocean' | 'green';
type NeutralPalette = `${ThemeId}-neutral`;

export interface ThemeDefinition {
  id: ThemeId;
  label: string;
  primary: Palette;
  // Tinted neutral palette (see main.css). Swapped on `setTheme` so NuxtUI's
  // neutral-backed surfaces (`bg-default`, `bg-muted`, dividers, rings) pick
  // up the theme's hue instead of staying pure slate.
  neutral: NeutralPalette;
}

export const themes: ThemeDefinition[] = [
  {id: 'lavender', label: 'Lavender', primary: 'lavender', neutral: 'lavender-neutral'},
  {id: 'mint', label: 'Mint', primary: 'mint', neutral: 'mint-neutral'},
  {id: 'peach', label: 'Peach', primary: 'peach', neutral: 'peach-neutral'},
  {id: 'blush', label: 'Blush', primary: 'blush', neutral: 'blush-neutral'},
  {id: 'ocean', label: 'Ocean', primary: 'ocean', neutral: 'ocean-neutral'},
  {id: 'nuxt', label: 'Nuxt', primary: 'green', neutral: 'nuxt-neutral'},
];

export const swatchFor = (palette: Palette): string => `var(--color-${palette}-500)`;

const STORAGE_KEY = 'centsible.theme';
const DEFAULT_THEME_ID: ThemeId = 'blush';

const safeStorage = {
  get(key: string): string | null {
    try { return localStorage.getItem(key); } catch { return null; }
  },
  set(key: string, value: string): void {
    try { localStorage.setItem(key, value); } catch { /* private mode / quota */ }
  },
};

export const useTheme = () => {
  const appConfig = useAppConfig();
  const colorMode = useColorMode();
  const currentThemeId = useState<ThemeId>('centsible.theme.id', () => DEFAULT_THEME_ID);

  const current = computed<ThemeDefinition>(
    () => themes.find(t => t.id === currentThemeId.value) ?? themes[0]!,
  );

  const mode = computed<ThemeMode>({
    get: () => (colorMode.preference as ThemeMode) ?? 'system',
    set: (value) => { colorMode.preference = value; },
  });

  function setTheme(id: ThemeId) {
    const theme = themes.find(t => t.id === id);
    if (!theme || theme.id === currentThemeId.value) {
      return;
    }

    currentThemeId.value = theme.id;
    // NuxtUI's color plugin watches `appConfig.ui.colors`; mutating here swaps
    // `--ui-color-primary-*` and `--ui-color-neutral-*` to the new palettes
    // without a reload.
    const colors = appConfig.ui.colors as Record<string, string>;
    colors.primary = theme.primary;
    colors.neutral = theme.neutral;

    if (import.meta.client) {
      safeStorage.set(STORAGE_KEY, theme.id);
    }
  }

  function restore() {
    if (!import.meta.client) {
      return;
    }
    const stored = safeStorage.get(STORAGE_KEY);
    const target = stored && themes.some(t => t.id === stored)
      ? (stored as ThemeId)
      : DEFAULT_THEME_ID;
    setTheme(target);
  }

  return {themes, current, mode, setTheme, restore};
};
