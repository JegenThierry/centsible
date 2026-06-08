export default defineAppConfig({
  ui: {
    colors: {
      primary: 'blush',
      neutral: 'blush-neutral',
    },

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
