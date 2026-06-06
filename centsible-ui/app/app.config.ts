export default defineAppConfig({
  ui: {
    // Brand palette — swapped at runtime by `useTheme` (composables/use-theme.ts).
    // `primary` is reserved for accents, CTAs and active states; `neutral` carries every surface.
    colors: {
      primary: 'blush',
      neutral: 'blush-neutral',
    },

    // Flat surface language: solid `bg-default`, a hairline `ring-default` border and a subtle
    // shadow for gentle elevation — no translucency or backdrop-blur. Overlay components below
    // (modal, popover, menus, toast, ...) intentionally inherit Nuxt UI's defaults, which already
    // ship this exact treatment (`bg-default shadow-lg ring ring-default`), so they need no override.
    card: {
      variants: {
        variant: {
          outline: {
            root: 'bg-default ring ring-default divide-y divide-default shadow-sm',
          },
        },
      },
    },

    pageCard: {
      variants: {
        variant: {
          outline: {
            root: 'bg-default ring ring-default shadow-sm',
          },
        },
      },
    },

    empty: {
      slots: {
        avatar: 'shrink-0 mb-2 bg-primary/10 dark:bg-primary/20 *:text-primary',
      },
      variants: {
        variant: {
          outline: {
            root: 'bg-default ring ring-default shadow-sm',
          },
        },
      },
    },

    // Wayfinding: the active item gets a subtle primary pill; inactive items hover neutral.
    navigationMenu: {
      compoundVariants: [
        {
          variant: 'pill',
          active: true,
          highlight: false,
          class: {link: 'before:bg-primary/10 dark:before:bg-primary/20'},
        },
        {
          variant: 'pill',
          active: true,
          highlight: true,
          disabled: false,
          class: {link: 'hover:before:bg-primary/15 dark:hover:before:bg-primary/25'},
        },
        {
          disabled: false,
          active: false,
          variant: 'pill',
          class: {link: 'hover:before:bg-primary/5 dark:hover:before:bg-primary/10'},
        },
        {
          disabled: false,
          variant: 'pill',
          highlight: false,
          active: false,
          orientation: 'horizontal',
          class: {link: 'data-[state=open]:before:bg-primary/5'},
        },
      ],
      variants: {
        active: {
          true: {
            childLink: 'before:bg-primary/10 dark:before:bg-primary/20 text-highlighted',
          },
          false: {
            childLink: [
              'hover:before:bg-primary/5 dark:hover:before:bg-primary/10 text-default hover:text-highlighted',
              'transition-colors before:transition-colors',
            ],
          },
        },
      },
    },

    // Menus keep a primary accent on the *selected* item only; hover stays neutral (Nuxt UI default).
    dropdownMenu: {
      variants: {
        active: {
          true: {
            item: 'text-highlighted before:bg-primary/10 dark:before:bg-primary/20',
          },
        },
      },
    },

    contextMenu: {
      variants: {
        active: {
          true: {
            item: 'text-highlighted before:bg-primary/10 dark:before:bg-primary/20',
          },
        },
      },
    },
  },
});
