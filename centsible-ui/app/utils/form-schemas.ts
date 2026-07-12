import {z} from 'zod';
import {ICON_PATTERN} from '~/utils/validation';
import {AMOUNT_INPUT, MONEY_FIELD_MAX} from '~/utils/money';
import {Frequency} from '~/models/recurring/recurring-transaction';

/**
 * The component-local `t` from `useI18n()`. Typed off `useI18n` itself so callers can pass their
 * `t` straight through without overload-assignability friction.
 */
type TranslateFn = ReturnType<typeof useI18n>['t'];

// --- field builders ---------------------------------------------------------
// Small zod fragments for the patterns that recurred across every create/edit form schema.

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

// --- per-domain factories ---------------------------------------------------
// One schema per domain, shared by the create and edit modals (which were byte-identical).

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

export function recurringSchema(t: TranslateFn) {
  const amountLabel = t('transactions.recurring.form.amount');
  const descriptionLabel = t('transactions.recurring.form.description');
  return z.object({
    amount: boundedNumber(t, amountLabel, AMOUNT_INPUT.min, AMOUNT_INPUT.max),
    description: requiredString(t, descriptionLabel, 255),
    frequency: z.nativeEnum(Frequency, {message: t('common.validation.required', {field: t('transactions.recurring.form.frequency')})}),
    startDate: z.string().min(1, t('common.validation.required', {field: t('transactions.recurring.form.startDate')})),
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
