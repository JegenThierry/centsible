/*
 * Component theming for the blush palette. We re-route surfaces that Nuxt UI
 * defaults to neutral (`bg-default`, `bg-elevated`) toward `primary` tints so
 * cards, nav links, dropdown items, and popovers all feel part of the same theme.
 * Semantic states (success/warning/error) stay untouched.
 */
export default defineAppConfig({
  ui: {
    colors: {
      // Initial values match the default theme; `useTheme().setTheme` swaps
      // both at runtime to keep neutrals tinted with the active theme's hue.
      primary: 'blush',
      neutral: 'blush-neutral',
    },

    card: {
      variants: {
        variant: {
          outline: {
            root: 'bg-default/80 dark:bg-default/60 backdrop-blur-sm ring ring-primary/15 dark:ring-primary/25 divide-y divide-primary/10 dark:divide-primary/15',
          },
        },
      },
    },

    pageCard: {
      variants: {
        variant: {
          outline: {
            root: 'bg-default/80 dark:bg-default/60 backdrop-blur-sm ring ring-primary/15 dark:ring-primary/25',
          },
        },
      },
    },

    empty: {
      slots: {
        // Tints the leading UAvatar so empty-state icons feel themed. The
        // `*:text-primary` overrides UAvatar's inner `text-muted` icon slot.
        avatar: 'shrink-0 mb-2 bg-primary/10 dark:bg-primary/20 *:text-primary',
      },
      variants: {
        variant: {
          outline: {
            root: 'bg-default/80 dark:bg-default/60 backdrop-blur-sm ring ring-primary/15 dark:ring-primary/25',
          },
        },
      },
    },

    navigationMenu: {
      compoundVariants: [
        // Active pill — primary-tinted instead of neutral elevated.
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
        // Inactive hover — soft primary wash.
        {
          disabled: false,
          active: false,
          variant: 'pill',
          class: {link: 'hover:before:bg-primary/5 dark:hover:before:bg-primary/10'},
        },
        // Open state (parent of expanded submenu).
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

    dropdownMenu: {
      slots: {
        content: 'bg-default/90 dark:bg-default/80 backdrop-blur-sm ring ring-primary/15 dark:ring-primary/25',
      },
      variants: {
        active: {
          true: {
            item: 'text-highlighted before:bg-primary/10 dark:before:bg-primary/20',
          },
          false: {
            item: [
              'text-default data-highlighted:text-highlighted data-[state=open]:text-highlighted data-highlighted:before:bg-primary/5 dark:data-highlighted:before:bg-primary/10 data-[state=open]:before:bg-primary/5',
              'transition-colors before:transition-colors',
            ],
          },
        },
      },
    },

    // Overlay surfaces that default to plain `bg-default` — give them the same
    // translucent, primary-ringed treatment as `card` so they sit cohesively
    // on top of the blush-tinted body background.
    modal: {
      slots: {
        content: 'bg-default/90 dark:bg-default/80 backdrop-blur-sm ring ring-primary/15 dark:ring-primary/25 divide-y divide-primary/10 dark:divide-primary/15',
        header: 'border-b-0',
        footer: 'border-t-0',
      },
    },

    slideover: {
      slots: {
        content: 'bg-default/90 dark:bg-default/80 backdrop-blur-sm ring ring-primary/15 dark:ring-primary/25 divide-y divide-primary/10 dark:divide-primary/15',
        header: 'border-b-0',
        footer: 'border-t-0',
      },
    },

    drawer: {
      slots: {
        content: 'bg-default/90 dark:bg-default/80 backdrop-blur-sm ring ring-primary/15 dark:ring-primary/25',
      },
    },

    popover: {
      slots: {
        content: 'bg-default/90 dark:bg-default/80 backdrop-blur-sm ring ring-primary/15 dark:ring-primary/25',
      },
    },

    tooltip: {
      slots: {
        content: 'bg-default/95 dark:bg-default/85 backdrop-blur-sm ring ring-primary/20 dark:ring-primary/30',
      },
    },

    contextMenu: {
      slots: {
        content: 'bg-default/90 dark:bg-default/80 backdrop-blur-sm ring ring-primary/15 dark:ring-primary/25',
      },
      variants: {
        active: {
          true: {
            item: 'text-highlighted before:bg-primary/10 dark:before:bg-primary/20',
          },
          false: {
            item: [
              'text-default data-highlighted:text-highlighted data-[state=open]:text-highlighted data-highlighted:before:bg-primary/5 dark:data-highlighted:before:bg-primary/10 data-[state=open]:before:bg-primary/5',
              'transition-colors before:transition-colors',
            ],
          },
        },
      },
    },

    selectMenu: {
      slots: {
        content: 'bg-default/90 dark:bg-default/80 backdrop-blur-sm ring ring-primary/15 dark:ring-primary/25',
      },
    },

    commandPalette: {
      slots: {
        root: 'bg-default/90 dark:bg-default/80 backdrop-blur-sm ring ring-primary/15 dark:ring-primary/25',
      },
    },

    toast: {
      slots: {
        root: 'bg-default/95 dark:bg-default/85 backdrop-blur-sm ring ring-primary/20 dark:ring-primary/30',
      },
    },
  },
});
