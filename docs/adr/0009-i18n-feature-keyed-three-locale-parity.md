# ADR-0009: i18n — Feature-Keyed Strings with Three-Locale Parity

Status:  Accepted
Date:    2026-06-06
Scope:   frontend (with backend message keys)

## Context

Centsible ships in English, French, and German. Hardcoded strings break translation, and
a key that exists in one locale but not another produces missing-text bugs that only
surface for some users.

## Decision

- **No hardcoded user-facing strings** in components. Resolve text through
  `const { t } = useI18n()` and reference a key: `{{ t('budgets.page.title') }}`.
- Strings live in `centsible-ui/i18n/locales/<en|fr|de>/<feature>.json`, **split one file
  per feature** (`budgets.json`, `transactions.json`, …). Keys are **nested by feature**:
  `budgets.list.emptyTitle`.
- **Three-locale parity is mandatory.** Every key exists in `en`, `fr`, and `de` with an
  identical structure. When you add or rename a key, update all three.
- Backend message keys thrown via `LocalizedException`
  ([ADR-0004](0004-localized-exception-hierarchy.md)) follow the same parity rule in the
  server-side bundles.

## Examples

**Do**
- Add `budgets.delete.confirm` to `en/budgets.json`, `fr/budgets.json`, and
  `de/budgets.json` in the same edit.
- Keep keys scoped: `profile.form.firstNameLabel`, not a flat `firstNameLabel`.

**Don't**
- Write `<h1>Budgets</h1>` or `toast.error('Failed to save')` with literal copy.
- Add a key to `en/` only and rely on a fallback.
- Dump unrelated strings into `common.json` — put them in their feature file.

## Consequences

- Adding a language is a matter of supplying one more locale folder.
- Missing-translation bugs are caught structurally (parity), not at runtime by a user.
- Cost: every string touches three files. This is the explicit trade for reliable i18n.
