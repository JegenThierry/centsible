import {z} from 'zod';
import {ICON_PATTERN} from '~/utils/validation';
import {AMOUNT_INPUT, BALANCE_INPUT, MONEY_FIELD_MAX} from '~/utils/money';
import {Frequency} from '~/models/recurring/recurring-transaction';

/**
 * The component-local `t` from `useI18n()`. Typed off `useI18n` itself so callers can pass their
 * `t` straight through without overload-assignability friction.
 */
type TranslateFn = ReturnType<typeof useI18n>['t'];

/** Trimmed, required text: non-empty and length-capped, with localized messages. */
export function requiredString(t: TranslateFn, label: string, max: number) {
  return z.string().trim()
    .min(1, t('common.validation.required', {field: label}))
    .max(max, t('common.validation.maxLength', {field: label, max}));
}

/** Trimmed, optional text: length-capped only when present. */
export function optionalString(t: TranslateFn, label: string, max: number) {
  return z.string().trim()
    .max(max, t('common.validation.maxLength', {field: label, max}))
    .optional();
}

/** Coerced number bounded to [min, max], with localized number/min/max messages. */
export function boundedNumber(t: TranslateFn, label: string, min: number, max: number) {
  return z.coerce.number({message: t('common.validation.number', {field: label})})
    .min(min, t('common.validation.min', {field: label, min}))
    .max(max, t('common.validation.max', {field: label, max}));
}

/**
 * Bounded number that's simply absent when the field is blank — number inputs emit `''` when
 * cleared, which would otherwise reach the backend and fail BigDecimal parsing.
 */
export function optionalBoundedNumber(t: TranslateFn, label: string, min: number, max: number) {
  return z.preprocess(
    (v) => (v === '' || v === undefined || v === null) ? undefined : v,
    boundedNumber(t, label, min, max).optional(),
  );
}

/** Required ISO date string. */
export function requiredDate(t: TranslateFn, label: string) {
  return z.string().min(1, t('common.validation.required', {field: label}));
}

/**
 * Required id-valued picker (a select bound to `string | undefined`). Localizes the missing-value
 * case as well as the empty-string one — a bare `z.string()` would report zod's English default
 * when the field was never touched.
 */
export function requiredSelection(t: TranslateFn, label: string) {
  const message = t('common.validation.required', {field: label});
  return z.string({message}).min(1, message);
}

/** Required object-valued picker — a whole `Category`/`Contact`, not its id. */
export function requiredObject(t: TranslateFn, label: string) {
  return z.custom((v) => v != null && typeof v === 'object', {
    message: t('common.validation.required', {field: label}),
  });
}

export function contactSchema(t: TranslateFn) {
  const firstNameLabel = t('contacts.form.firstNameLabel');
  const lastNameLabel = t('contacts.form.lastNameLabel');
  return z.object({
    firstName: requiredString(t, firstNameLabel, 100),
    lastName: optionalString(t, lastNameLabel, 100),
  });
}

export function categorySchema(t: TranslateFn) {
  const nameLabel = t('categories.form.nameLabel');
  const iconLabel = t('categories.form.iconLabel');
  return z.object({
    name: requiredString(t, nameLabel, 50),
    icon: requiredString(t, iconLabel, 50)
      .regex(ICON_PATTERN, t('categories.form.iconPatternMessage')),
  });
}

export function budgetSchema(t: TranslateFn) {
  const categoryLabel = t('budgets.form.categoryLabel');
  const limitLabel = t('budgets.form.limitLabel');
  return z.object({
    category: z.any().refine((v) => !!v, t('common.validation.required', {field: categoryLabel})),
    amountLimit: boundedNumber(t, limitLabel, 0.01, MONEY_FIELD_MAX),
  });
}

/** Shared by the create modal's standard mode and the edit modal — they validate the same entity. */
export function transactionSchema(t: TranslateFn) {
  return z.object({
    category: requiredObject(t, t('transactions.form.category')),
    amount: boundedNumber(t, t('transactions.form.amount'), AMOUNT_INPUT.min, AMOUNT_INPUT.max),
    description: requiredString(t, t('transactions.form.description'), 255),
    transactionDate: requiredDate(t, t('transactions.form.date')),
  });
}

/** Shared by the create modal's transfer mode and the edit-transfer modal. */
export function transferSchema(t: TranslateFn) {
  return z.object({
    sourceAccountId: requiredSelection(t, t('transactions.transfer.fromAccount')),
    destinationAccountId: requiredSelection(t, t('transactions.transfer.toAccount')),
    amount: boundedNumber(t, t('transactions.transfer.amount'), AMOUNT_INPUT.min, AMOUNT_INPUT.max),
    description: requiredString(t, t('transactions.form.description'), 255),
    transactionDate: requiredDate(t, t('transactions.form.date')),
  });
}

/** Balance correction: [newBalance] is an absolute target, so it may legitimately be zero or negative. */
export function setBalanceSchema(t: TranslateFn) {
  return z.object({
    newBalance: boundedNumber(t, t('transactions.form.modes.setBalance.newBalance'), BALANCE_INPUT.min, BALANCE_INPUT.max),
    category: requiredObject(t, t('transactions.form.category')),
    description: requiredString(t, t('transactions.form.description'), 255),
    transactionDate: requiredDate(t, t('transactions.form.date')),
  });
}

export function loanSchema(t: TranslateFn) {
  const contactLabel = t('contacts.loans.form.pickContact');
  const accountLabel = t('contacts.loans.form.fromAccountLabel');
  return z.object({
    contactId: z.string().optional(),
    newContactFirstName: optionalString(t, t('contacts.loans.form.newFirstNameLabel'), 100),
    newContactLastName: optionalString(t, t('contacts.loans.form.newLastNameLabel'), 100),
    accountId: z.string().optional(),
    affectBalance: z.boolean(),
    lentAmount: boundedNumber(t, t('contacts.loans.form.lentLabel'), 0.01, MONEY_FIELD_MAX),
    owedAmount: boundedNumber(t, t('contacts.loans.form.owedLabel'), 0, MONEY_FIELD_MAX),
    interestRate: optionalBoundedNumber(t, t('contacts.loans.form.interestRateLabel'), 0, 999.99),
    description: requiredString(t, t('contacts.loans.form.descriptionLabel'), 255),
    transactionDate: requiredDate(t, t('contacts.loans.form.dateLabel')),
    dueDate: z.string().optional(),
    notes: optionalString(t, t('contacts.loans.form.notesLabel'), 500),
  })
    .refine((d) => !!d.contactId || !!d.newContactFirstName?.trim(), {
      message: t('common.validation.required', {field: contactLabel}),
      path: ['contactId'],
    })
    .refine((d) => !d.affectBalance || !!d.accountId, {
      message: t('common.validation.required', {field: accountLabel}),
      path: ['accountId'],
    });
}

export function recurringSchema(t: TranslateFn) {
  const amountLabel = t('transactions.recurring.form.amount');
  const descriptionLabel = t('transactions.recurring.form.description');
  return z.object({
    amount: boundedNumber(t, amountLabel, AMOUNT_INPUT.min, AMOUNT_INPUT.max),
    description: requiredString(t, descriptionLabel, 255),
    frequency: z.nativeEnum(Frequency, {message: t('common.validation.required', {field: t('transactions.recurring.form.frequency')})}),
    startDate: requiredDate(t, t('transactions.recurring.form.startDate')),
    isTransfer: z.boolean(),
    category: z.any().optional(),
    sourceAccountId: z.string().optional(),
    destinationAccountId: z.string().optional(),
  }).superRefine((d, ctx) => {
    if (d.isTransfer) {
      if (!d.sourceAccountId) {
        ctx.addIssue({code: z.ZodIssueCode.custom, path: ['sourceAccountId'], message: t('common.validation.required', {field: t('transactions.transfer.fromAccount')})});
      }
      if (!d.destinationAccountId) {
        ctx.addIssue({code: z.ZodIssueCode.custom, path: ['destinationAccountId'], message: t('common.validation.required', {field: t('transactions.transfer.toAccount')})});
      }
    } else if (!d.category) {
      ctx.addIssue({code: z.ZodIssueCode.custom, path: ['category'], message: t('common.validation.required', {field: t('transactions.recurring.form.category')})});
    }
  });
}
