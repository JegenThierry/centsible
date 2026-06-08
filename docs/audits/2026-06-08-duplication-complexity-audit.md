# Centsible — Duplication & Complexity Audit

_2026-06-08 · branch `develop` · whole monorepo (Kotlin/Spring backend + Nuxt/Vue/TS frontend)_

## How this was produced

Fan-out of **33 agents**: 16 area finders → one adversarial verifier per area → one synthesis pass.
- **Excluded:** generated jOOQ/proto sources.
- **ADR-aware:** intentional patterns were deliberately _not_ flagged — per-layer naming (ADR-0002), one-submodule-per-provider/format SPI structure (ADR-0005), and the templated FE store/service/model shape (ADR-0006/0008).
- **Read-only:** no code was changed. This is a report + ordered fix plan only.

> **Caveat — read before trusting the list.** The per-area verifiers **rejected 0 of 36** candidates; they tightened confidence/severity rather than culling. So: the **24 high/medium-confidence** items are solid (many carry _confirmed drift between copies_, the strongest evidence that the duplication is a live liability). Treat the **12 low-confidence** items as candidates to confirm at implementation time, not settled facts.

## At a glance

- **36 confirmed findings** — 31 duplication, 5 complexity.
- **Severity:** 7 high · 17 medium · 12 low.
- Collapse into **9 themes**. Two dominate:
  1. **Create/Edit/Delete modal + schema boilerplate** — byte-identical zod schemas and a copy-pasted loading/try-catch/`UModal`/`UForm` shell across ~17 organism modals.
  2. **Money / currency / password / date rules** hand-copied across backend Bean Validation, three core services, the import/integration plugins, and ~11 frontend zod/HTML sites — with no single source of truth.

## Correctness, not just cleanliness (worth doing regardless of dedup appetite)

These are latent bugs the duplication is actively hiding:

1. **ADR-0015 transfer-exclusion is one missed predicate from a silent reporting bug.** The split-aware EXPENSE aggregate (incl. `TRANSFER_GROUP_ID IS NULL`) is hand-rewritten in 3 repos — `BudgetRepository.kt:87-102`, `ReportsRepository.kt:53-74`, `TransactionRepository.kt:383-398`. Drop the predicate at one site and budgets/reports silently double-count transfers. → fix-plan **#3**.
2. **ADR-0004 leak.** `RecurringTransactionService.assertCategoryOwned` (`RecurringTransactionService.kt:154-160`) throws a raw `IllegalArgumentException` for the exact condition the other three services raise as `LocalizedException.BadRequest("error.category.notAccessible")` — an unlocalized error escaping the handler. → fix-plan **#10**.
3. **Password UX gap.** The live strength checklist and the submit-time zod validator are separate copies (`register-password-input.vue:12-17`, `password-change-section.vue:13,31-42`, `auth/create-form.vue:55-64`, `auth/reset-password-card.vue:34-42`); change the special-char set in one and the checklist green-lights a password the schema rejects. → fix-plan **#4**.
4. **Silent mapping default.** `csvProbe` hand-maps core→DTO field-by-field (`ImportsResource.kt:69-94`); a forgotten field compiles fine and maps to a default instead of failing. → fix-plan **#9**.

**Already-diverged copies** (drift confirmed by the agents): money min `0.00` vs `0.01` on owed-amount DTOs · `"eur"` parses to `EUR` in two services and `null` in a third · conversion-preview hint colour `text-neutral-400` vs `text-muted` · lendings export filename uses contact name in PDF but the contact UUID in CSV/JSON.

## Coverage (confirmed / rejected per area)

| Area | Confirmed | Rejected | | Area | Confirmed | Rejected |
| --- | --- | --- | --- | --- | --- | --- |
| api | 3 | 0 | | org-A | 7 | 0 |
| core | 4 | 0 | | org-B | 4 | 0 |
| rest | 1 | 0 | | molecules | 5 | 0 |
| export | 2 | 0 | | atoms | 3 | 0 |
| jooq | 3 | 0 | | fe-data | 2 | 0 |
| integrations | 2 | 0 | | fe-pages-utils | 2 | 0 |
| imports | 1 | 0 | | x-frontend | 5 | 0 |
| x-backend | 3 | 0 | | x-crossstack-config | 3 | 0 |

## Ordered fix plan

Ordered by (impact × confidence) ÷ effort, low-risk high-value first. Backend and frontend halves of most steps are independent and can land in parallel.

1. **Delete dead `nav/profile.vue`** _(small / low risk)_ — confirmed dead (no references) and a divergent copy of the sidebar user-menu. Zero behavioural risk; shrinks the modal/organism surface before the bigger refactors.
   `centsible-ui/app/components/_organisms/nav/profile.vue`
2. **Centralize money/limit constants + currency parsing** _(medium / low risk)_ — add `LIMIT_INPUT = {0.01, 9_999_999.99}` to `money.ts` and reference it at the ~11 FE sites; add `Currency.parseOrNull` on the api companion and route the 3 core parse sites through it (fixes the `eur`→`null` drift); drop the `DefaultCurrencyUpdateDTO` regex in favour of the `Currency` enum. Prerequisite for the schema-factory work (#5).
   `money.ts`, budgets/loans modals, `Currency.kt`, `DefaultCurrencyUpdateDTO.kt`, `LoanService.kt`, `ProviderSyncOrchestrator.kt`, `TransactionService.kt`
3. **Extract shared jOOQ ownership + EXPENSE-aggregation helpers** _(medium / medium risk)_ — highest backend correctness leverage (ADR-0015). Ownership subquery (8×) first → `ExpenseAggregation` object (3 repos) → fold `aggregateByMonth`/`aggregateByDay` onto a `periodKey` helper. `RepositoryHelpers.kt` already exists. Needs a migrated Postgres for the integration tests.
   `RepositoryHelpers.kt`, `TransactionRepository.kt`, `RecurringTransactionRepository.kt`, `BudgetRepository.kt`, `ReportsRepository.kt`
4. **Unify the password policy** _(medium / low risk)_ — define the policy once in `app/utils/validation.ts` (pattern + checklist array + combined predicate + `passwordsMatch` refine), consume in the 4 UI sites; in parallel extract a `@StrongPassword` composed constraint for the two backend DTOs. Self-contained.
   `validation.ts`, register-password-input, password-change-section, auth/create-form, auth/reset-password-card, `AuthRegisterRequest.kt`, `PasswordResetConfirmRequest.kt`
5. **Add zod validation-field builders + per-domain schema factories** _(medium / low risk)_ — builds on #2/#4. `requiredString`/`optionalString`/`boundedNumber` helpers, then one schema factory per domain (budget/recurring/contact/category + transfer/loan), imported by both modals of each pair — collapses ~20 inline validators and the byte-identical create/edit schemas (recurring's `superRefine` is the riskiest). Pure `tsc`-gated.
   `validation.ts`, models/recurring, budgets/recurring/contacts/categories modals, transfer-form, molecules/categories
6. **Convert the 3 hand-rolled delete modals to `ConfirmationModal`** _(small / low risk)_ — `ConfirmationModal` already owns the loading/try-catch/adze/toast delete flow (delete-budget/recurring prove it). De-risks #7 by shrinking the modal set.
   delete-category / delete-contact / delete-loan modals, confirmation-modal
7. **Collapse create+edit modal pairs + extract a `FormModal` shell** _(large / medium risk)_ — the largest single dedup; sequenced after #5/#6 so the shell only parameterizes the save body + reset/dirty-guard hook. Extract a `FormModal` molecule (loading + try/catch/finally + `UModal`/`UForm`/`ModalFooterActions` + reset-on-open), collapse each domain's create+edit into one `<X>Modal` with an optional entity prop, add `toRecurringPayload`. ~17 files.
   organisms/modals, categories/budgets/contacts/recurring/loans/transactions modals
8. **Extract shared UI formatting/preview/chart helpers** _(medium / low risk)_ — `formatCurrency` in `money.ts` (route `balance-number-format` + 3 inline `Intl.NumberFormat` sites through it); `<ConversionPreviewHint>`; `formatMonthYearLabel` options arg (delete the chart's local copy); `baseCurrencyChartOptions()` in use-chart-theme; `effectiveLimit`/`budgetBarColor` in models/budget. Independent leaf changes.
   `money.ts`, `date.ts`, use-chart-theme, models/budget, balance-number-format, transactions/loans/budgets molecules, dashboard + reports charts
9. **Extract shared helpers in the plugin-SPI modules** _(medium / low risk)_ — ADR-0005 already designates these shared modules. `toImportRow`/`logWarningSummary`/`requireDefaultCategoryId`/`parseDateOnlyAtUtc` into imports/core + integrations/support; an `integrations.http` `@ConfigurationProperties` timeout bean; per-export-type parse helpers in `RenderingHelpers` (fixes the triplicated not-found path and the lendings filename inconsistency). Also the `csvProbe` Konvert reverse mapper.
   imports/core, csv/ofx parsers, integrations/support + 3 provider configs, export/render, ImportsResource
10. **Backend service-layer dedup** _(medium / medium risk)_ — one category-ownership guard throwing `LocalizedException` uniformly (fixes the ADR-0004 leak); shared create/update transaction prologue/epilogue; `EnvBackedBytesEncryptor` base for the two ciphers; `RuleField.validOperators` single source for `RuleService`. Cover with existing core `*ServiceTest` (no DB).
   TransactionService, BudgetService, RecurringTransactionService, RuleService, RuleMatching, TotpSecretCipher, CredentialCipher
11. **Backend annotation/projection + lower-value FE tier cleanups** _(large / low risk)_ — `@MoneyAmount`/`@SmallMoneyAmount` across 9 form DTOs; delete `AttachmentRepository` manual DSL constants (use generated `TRANSACTION_ATTACHMENTS`); extract Contacts/Budget SELECT projections. FE: the entity-select atom (drops category-select's casts), the `app-*` passthrough factory (weigh the typing tradeoff), `totp-section` `runWithCode`, lift the 3 ADR-0006-violating molecules, the `runMutation` composable, and the store `loading`/`pending` rename. Several are subsumed by earlier steps.
   api/model, AttachmentRepository, ContactsRepository, BudgetRepository, _atoms/inputs + _atoms/ui, totp-section, budget/recurring/contact molecules, stores

## Findings by theme

### [HIGH] Money & currency rules duplicated across layers and languages

The monetary min/max/scale bounds, the supported-currency set, and safe Currency parsing are each re-encoded in many places spanning Kotlin Bean Validation, three core services, and ~11 frontend zod/HTML sites, with confirmed drift (mins of 0.00 vs 0.01; 'eur' parsing to EUR vs null). No single source of truth exists for either the bounds or the currency enum's parse/membership logic.

**Budget/loan money bounds (0.01..9999999.99) hand-copied across backend Bean Validation, frontend zod and HTML :min/:max, bypassing money.ts**  
`duplication · sev:medium · conf:high · effort:medium`
- _Evidence:_ money.ts defines only AMOUNT_INPUT (0.01..999999999999.99) and BALANCE_INPUT; the budget/loan tier 0.01..9999999.99 is absent and the literal 9999999.99 is inlined at 11 confirmed FE sites plus the two Kotlin annotations. A change to the cap requires editing the Kotlin annotation plus ~11 unrelated FE literals.
- _Fix:_ Add LIMIT_INPUT = {min: 0.01, max: 9_999_999.99} to money.ts and reference it from every budget/loan zod .min/.max and :min/:max binding (as AMOUNT_INPUT/BALANCE_INPUT already are); the backend annotation pair is the unavoidable cross-language copy.
- _Locations:_ centsible-ui/app/utils/money.ts:1-9; centsible-ui/app/components/_organisms/budgets/modals/create-budget-modal.vue:34-36; centsible-ui/app/components/_organisms/budgets/modals/edit-budget-modal.vue:32-34; centsible-ui/app/components/_organisms/loans/modals/create-loan-modal.vue:49-58; centsible-ui/app/components/_organisms/loans/modals/edit-loan-modal.vue:41-43; centsible-ui/app/components/_organisms/loans/modals/edit-loan-modal.vue:120; centsible-ui/app/components/_molecules/budgets/budget-form.vue:75-76; centsible-ui/app/components/_molecules/loans/loan-amount-fields.vue:28-29; centsible-ui/app/components/_molecules/loans/loan-amount-fields.vue:48-50; centsible-ui/app/components/_molecules/loans/repayment-form.vue:80-81; centsible-ui/app/components/_organisms/transactions/modals/create-transaction-modal.vue:120-123; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/budget/BudgetForm.kt:16-18; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/loan/LoanForm.kt:30-39

**Monetary-amount validation block (magic min/max/digits + message keys) copy-pasted across 9 backend form DTOs**  
`duplication · sev:low · conf:medium · effort:medium`
- _Evidence:_ Transaction-scale block (DecimalMin 0.01 / DecimalMax 999999999999.99 / Digits 12,2) appears verbatim in 5 sites; money-scale variant (DecimalMax 9999999.99 / Digits 7,2) in 5 sites. Confirmed drift: owed/owed-update use DecimalMin 0.00 while the rest use 0.01, and ImportTransactionRow omits the i18n message keys, so no single composite covers the min uniformly.
- _Fix:_ Introduce composed @MoneyAmount(integer=12) and @SmallMoneyAmount(integer=7) meta-annotations carrying the shared DecimalMax/Digits + message keys; keep DecimalMin/NotNull on the composite where the min is uniform and apply DecimalMin 0.00 separately on the two owed-amount fields.
- _Locations:_ centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/transaction/TransactionForm.kt:17-21; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/transaction/TransactionForm.kt:53-57; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/transaction/TransferForm.kt:14-18; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/recurring/RecurringTransactionForm.kt:16-20; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/transaction/ImportTransactions.kt:18-22; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/budget/BudgetForm.kt:15-18; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/loan/LoanForm.kt:29-39; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/loan/LoanUpdateForm.kt:23-27; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/loan/RepaymentForm.kt:18-21

**Supported-currency list duplicated between the Currency enum and a hardcoded regex**  
`duplication · sev:medium · conf:high · effort:small`
- _Evidence:_ DefaultCurrencyUpdateDTO.defaultCurrency is a String validated by a @Pattern regex listing exactly the 23 Currency enum entries (EUR..IDR) in the same order, while CreateBudgetAccountRequest and RecurringTransactionForm type the field as the Currency enum directly. The regex is a hand-maintained second copy that can silently drift from the enum.
- _Fix:_ Drop the regex: type defaultCurrency as Currency (matching the other request DTOs) so enum deserialization enforces membership, or validate the String against Currency.entries in the service.
- _Locations:_ centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/user/DefaultCurrencyUpdateDTO.kt:8-11; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/budgetaccount/Currency.kt:3-7

**Safe Currency.valueOf parsing hand-rolled in 3 services with behavioral drift**  
`duplication · sev:medium · conf:high · effort:small`
- _Evidence:_ LoanService.parseCurrency does NOT uppercase and defaults EUR; ProviderSyncOrchestrator.parseCurrency uppercases and defaults EUR; TransactionService.parseProviderCurrency trims, uppercases, warns and returns null on failure. The api Currency is a bare enum with no parse helper, so 'eur' parses to EUR in two sites and null in the third.
- _Fix:_ Add Currency.parseOrNull(code: String?): Currency? (trim + uppercase + runCatching{valueOf}) on a companion in centsible-api; rewrite the two EUR defaults as parseOrNull(code) ?: Currency.EUR and the transaction site as parseOrNull(code), keeping its warn log at the call site.
- _Locations:_ centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/loans/LoanService.kt:170-171; centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/integrations/ProviderSyncOrchestrator.kt:191-193; centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/transactions/TransactionService.kt:545-551; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/budgetaccount/Currency.kt:3-7

### [HIGH] Create/Edit/Delete modal boilerplate (shells, schemas, payload mappers) duplicated per domain

Across categories, budgets, contacts, recurring, loans and transactions, each create/edit modal pair re-declares a byte-identical zod schema, template body and save handler, and ~17 modals re-implement the same loading+try/catch+UModal/UForm shell that ConfirmationModal already proves can be centralized. This is the single largest cluster of frontend duplication.

**Create/Edit modal pairs are near-identical per domain (schema + template + save handler duplicated)**  
`duplication · sev:high · conf:high · effort:large`
- _Evidence:_ Within each domain create vs edit share a byte-identical zod schema and identical template body, differing only in the form seed (resetForm/makeBlank vs watch->toForm) and the store/service call. recurring is worst: the ~25-line superRefine schema AND the ~12-line service payload object are duplicated verbatim (diff exit 0).
- _Fix:_ Per domain collapse create+edit into one <X>Modal taking an optional entity prop (create when null) that resolves seed, the store/service call, and the i18n title/submit/toast prefix from a small map; the schema, the form-fields child and the template then live once.
- _Locations:_ centsible-ui/app/components/_organisms/categories/modals/create-category-modal.vue:31-72; centsible-ui/app/components/_organisms/categories/modals/edit-category-modal.vue:35-74; centsible-ui/app/components/_organisms/budgets/modals/create-budget-modal.vue:32-67; centsible-ui/app/components/_organisms/budgets/modals/edit-budget-modal.vue:30-70; centsible-ui/app/components/_organisms/contacts/modals/create-contact-modal.vue:21-49; centsible-ui/app/components/_organisms/contacts/modals/edit-contact-modal.vue:25-53; centsible-ui/app/components/_organisms/recurring/modals/create-recurring-modal.vue:35-128; centsible-ui/app/components/_organisms/recurring/modals/edit-recurring-modal.vue:37-130

**Form-modal shell (loading ref + try/catch/finally save + UModal/UForm/ModalFooterActions + reset-on-open) copy-pasted across ~17 modals**  
`duplication · sev:high · conf:medium · effort:large`
- _Evidence:_ grep confirms 17 organism modals match ModalFooterActions+UForm; each repeats loading=ref(false), a handleSave shaped loading=true / try await store-or-service / isOpen=false / catch adze-or-toastError / finally loading=false, and the UModal+UForm+ModalFooterActions template. confirmation-modal.vue:27-68 already centralizes exactly this flow for the delete path. Reset varies (watch(isOpen) vs useModalDirtyGuard) and the error half varies (adze.ns vs useApiErrors().toastError), so the molecule must parameterize both.
- _Fix:_ Extract a FormModal molecule (props: open model, title/description/submitLabel, schema, injected submit:()=>Promise<void>, optional reset/dirty-guard hook; default slot for fields) owning the loading ref, try/catch/finally, error feedback, isOpen=false on success and the UModal/UForm/ModalFooterActions markup; each modal then supplies only schema + fields + submit body.
- _Locations:_ centsible-ui/app/components/_organisms/categories/modals/create-category-modal.vue:25-110; centsible-ui/app/components/_organisms/budgets/modals/create-budget-modal.vue:26-87; centsible-ui/app/components/_organisms/contacts/modals/create-contact-modal.vue:15-69; centsible-ui/app/components/_organisms/loans/modals/create-loan-modal.vue:24-131; centsible-ui/app/components/_organisms/recurring/modals/create-recurring-modal.vue:28-150; centsible-ui/app/components/_organisms/modals/confirmation-modal.vue:27-68

**create/edit modal siblings carry byte-identical inline zod schemas with no shared schema factory**  
`duplication · sev:low · conf:high · effort:medium`
- _Evidence:_ Each create/edit pair declares a character-for-character identical const schema = z.object(...): contacts, categories (incl. ICON_PATTERN), budgets, and recurring (incl. the entire superRefine transfer-vs-category block) are identical between create and edit siblings. No shared z.object factory exists under app/utils or app/composables.
- _Fix:_ Extract one factory per domain (useBudgetSchema(t), useRecurringSchema(t), useContactSchema(t), useCategorySchema(t)) returning the z.object and import into both modals of each pair; prioritize recurring since its superRefine is the riskiest copy.
- _Locations:_ centsible-ui/app/components/_organisms/budgets/modals/create-budget-modal.vue:32-37; centsible-ui/app/components/_organisms/budgets/modals/edit-budget-modal.vue:30-35; centsible-ui/app/components/_organisms/recurring/modals/create-recurring-modal.vue:35-59; centsible-ui/app/components/_organisms/recurring/modals/edit-recurring-modal.vue:37-61; centsible-ui/app/components/_organisms/contacts/modals/create-contact-modal.vue:21-28; centsible-ui/app/components/_organisms/contacts/modals/edit-contact-modal.vue:25-32; centsible-ui/app/components/_organisms/categories/modals/create-category-modal.vue:31-39; centsible-ui/app/components/_organisms/categories/modals/edit-category-modal.vue:35-43

**Category create/edit modals inline an identical schema + form body instead of a shared fields molecule**  
`duplication · sev:medium · conf:high · effort:medium`
- _Evidence:_ Byte-identical between the two modals: CategoryForm default, the Zod schema, typeOptions, and the template body (AppRadioGroup + name BaseInput + IconInput + ColorSelect); only the open/category watcher and createCategory vs updateCategory differ. The codebase already factors the equivalent contact-form.vue and budget-form.vue molecules; only categories was left un-factored.
- _Fix:_ Extract _molecules/categories/category-form.vue (radio+name+icon+color) + a categoryFormSchema(t) helper, and have both modals render <CategoryForm v-model=form>, mirroring contact-form/budget-form.
- _Locations:_ centsible-ui/app/components/_organisms/categories/modals/create-category-modal.vue:18-101; centsible-ui/app/components/_organisms/categories/modals/edit-category-modal.vue:22-103

**Recurring create/edit modals duplicate the entire Zod schema and service-payload mapping**  
`duplication · sev:high · conf:high · effort:medium`
- _Evidence:_ The 25-line schema (amount/description/frequency/startDate + isTransfer/category superRefine) is byte-identical between create and edit (diff exit 0); the ~12-line service payload with the same isTransfer ternaries is byte-identical too, differing only in service.create(sourceAccountId,...) vs service.update(props.rule.id,...).
- _Fix:_ Extract buildRecurringSchema(t) (in models/recurring) or useRecurringSchema() and a shared toRecurringPayload(form) form->DTO mapper, imported by both modals.
- _Locations:_ centsible-ui/app/components/_organisms/recurring/modals/create-recurring-modal.vue:35-59; centsible-ui/app/components/_organisms/recurring/modals/edit-recurring-modal.vue:37-61; centsible-ui/app/components/_organisms/recurring/modals/create-recurring-modal.vue:107-119; centsible-ui/app/components/_organisms/recurring/modals/edit-recurring-modal.vue:109-121

**Transfer-form Zod schema + same-account guard duplicated between create-transaction and edit-transfer modals**  
`duplication · sev:medium · conf:high · effort:small`
- _Evidence:_ Byte-identical transfer schema (sourceAccountId/destinationAccountId/amount via AMOUNT_INPUT/description/transactionDate, same i18n keys) between create:86-97 and edit-transfer:33-44; the same-account guard if(sourceId===destinationId) toasts.error(...) is duplicated in both save handlers. Both render the same TransferFormFields molecule.
- _Fix:_ Co-locate transferFormSchema(t) with _molecules/transactions/transfer-form.vue and import in both modals; optionally a shared isSameAccount guard alongside it.
- _Locations:_ centsible-ui/app/components/_organisms/transactions/modals/create-transaction-modal.vue:86-97,251-254; centsible-ui/app/components/_organisms/transactions/modals/edit-transfer-modal.vue:33-44,90-93

**Loan-creation Zod schema duplicated between the loan modal and the transaction 'lending' mode**  
`duplication · sev:medium · conf:medium · effort:small`
- _Evidence:_ create-transaction's schemaLoan and create-loan-modal's schema encode the same loan contract (new contact names, lent/owed/interest bounds, description/notes, dates) plus the same two cross-field rules. Not byte-identical: create-loan uses superRefine + plain optional interestRate, the transaction modal uses two refines + an optionalNumber() preprocess, so a shared schema must preserve both nuances.
- _Fix:_ Define one loanFormSchema(t) co-located with the LoanForm model/molecule (settle on superRefine + optionalNumber preprocess) and consume it in both create-loan-modal and the lending branch.
- _Locations:_ centsible-ui/app/components/_organisms/loans/modals/create-loan-modal.vue:39-75; centsible-ui/app/components/_organisms/transactions/modals/create-transaction-modal.vue:112-138

**Three delete modals hand-roll the loading/try-catch flow that ConfirmationModal already provides**  
`duplication · sev:medium · conf:high · effort:small`
- _Evidence:_ delete-budget and delete-recurring are thin wrappers over ConfirmationModal (which owns loading+try/catch+adze+toasts). delete-category, delete-contact and delete-loan instead re-implement that flow by hand: own loading ref + handleDelete try/catch/finally + adze.ns(..).error + hand-built UModal+ModalFooterActions(submit-color='error'). Same logic, duplicated three more times (the contact modal is one of four such hand-rolls, not a lone outlier).
- _Fix:_ Rewrite delete-category/contact/loan to render ConfirmationModal with a :delete-callback (e.g. ()=>contactsStore.deleteContact(id)) and :entity/:body for the name-aware copy, matching delete-budget/recurring.
- _Locations:_ centsible-ui/app/components/_organisms/categories/modals/delete-category-modal.vue:15-43; centsible-ui/app/components/_organisms/contacts/modals/delete-contact-modal.vue:1-55; centsible-ui/app/components/_organisms/loans/modals/delete-loan-modal.vue:19-49; centsible-ui/app/components/_organisms/budgets/modals/delete-budget-modal.vue:19-30; centsible-ui/app/components/_organisms/recurring/modals/delete-recurring-modal.vue:20-31; centsible-ui/app/components/_organisms/modals/confirmation-modal.vue:33-49

### [HIGH] Password policy encoded in many UI sites

The password complexity rule (length/upper/lower/digit/special), the confirm-match refine, and the live strength checklist are each re-encoded across four auth/user components, so a live checklist can green-light a password the submit-time zod schema rejects. There is no shared password helper.

**Password policy (length/case/digit/special), confirm-match refine and strength checklist duplicated across 4 UI sites**  
`duplication · sev:high · conf:high · effort:medium`
- _Evidence:_ Merged from two area audits. The complexity refine (v.length>=8 && /[A-Z]/ && /[a-z]/ && /\d/ && /[@$!%*?&]/) is byte-identical in create-form.vue:56 and reset-password-card.vue:35; password-change-section.vue:13 re-encodes it as PASSWORD_PATTERN. The 4-item passwordRules checklist is byte-duplicated in register-password-input.vue:12-17 and password-change-section.vue:38-42. The 'passwords match' refine repeats in create-form, reset-password-card and password-change-section. The live checklist and the submit-time validator can drift (change the special-char set once and the UI greenlights what the schema rejects).
- _Fix:_ Define the policy once in app/utils (validation.ts): PASSWORD_PATTERN + an array of {labelKey, test:(v)=>boolean} + a combined predicate; render the checklist from it in register-password-input and password-change-section, derive every zod .refine() from the same predicate, and provide a shared passwordsMatch refine.
- _Locations:_ centsible-ui/app/components/_molecules/inputs/register-password-input.vue:12-17; centsible-ui/app/components/_organisms/user/password-change-section.vue:13; centsible-ui/app/components/_organisms/user/password-change-section.vue:31-42; centsible-ui/app/components/_organisms/auth/create-form.vue:55-64; centsible-ui/app/components/_organisms/auth/reset-password-card.vue:34-42

**Password-strength regex and max-length duplicated across the two backend password DTOs**  
`duplication · sev:low · conf:high · effort:small`
- _Evidence:_ Byte-identical: both fields carry @Size(max=72, {validation.password.tooLong}) + @Pattern(^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$, {validation.password.strength}). PasswordChangeRequest deliberately omits the pattern (strength enforced in the service), so unifying only the two annotation-based DTOs is the in-scope dedup.
- _Fix:_ Extract a composed @StrongPassword constraint (the regex + @Size(max=72) + both message keys) and apply it to AuthRegisterRequest.password and PasswordResetConfirmRequest.password.
- _Locations:_ centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/auth/AuthRegisterRequest.kt:16-21; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/auth/PasswordResetConfirmRequest.kt:12-18

### [HIGH] jOOQ ownership, transfer-leg and aggregation SQL duplicated across repositories

The ADR-0003 account-ownership subquery (8 sites), the ADR-0015 transfer-exclusion EXPENSE aggregation (3 repos), the income/expense CASE-sum + month-key, and the two-leg transfer insertion are each re-spelled in raw jOOQ rather than living in the existing RepositoryHelpers. Several carry ADR-0015 correctness invariants, so a fix applied to one site and missed in another is a silent reporting/budget bug.

**Account-ownership subquery predicate re-spelled 8x across two repositories**  
`duplication · sev:medium · conf:high · effort:small`
- _Evidence:_ The inner subquery select(ACCOUNTS.ID).from(ACCOUNTS).where(ACCOUNTS.USER_ID.eq(userId)) is byte-identical at all 8 sites; only the outer field (TRANSACTIONS.ACCOUNT_ID vs RECURRING_TRANSACTIONS.ACCOUNT_ID) differs. RepositoryHelpers.kt already exists and houses ensureAccountOwnedByUser.
- _Fix:_ Add internal fun DSLContext.accountsOwnedBy(userId: UUID) = select(ACCOUNTS.ID).from(ACCOUNTS).where(ACCOUNTS.USER_ID.eq(userId)) to RepositoryHelpers.kt and call it at all 8 sites; fully behavior-preserving.
- _Locations:_ centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/TransactionRepository.kt:155-160; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/TransactionRepository.kt:177-182; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/TransactionRepository.kt:302-307; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/TransactionRepository.kt:331-335; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/TransactionRepository.kt:371-378; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/TransactionRepository.kt:581-585; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/RecurringTransactionRepository.kt:153-157; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/RecurringTransactionRepository.kt:168-172

**Split-aware EXPENSE aggregation (coalesce + ADR-0015 transfer-exclusion predicate) hand-rewritten in 3 repositories**  
`duplication · sev:high · conf:high · effort:medium`
- _Evidence:_ Each site independently declares effectiveAmount=coalesce(SPLITS.AMOUNT, TRANSACTIONS.AMOUNT), effectiveCategoryId=coalesce(SPLITS.CATEGORY_ID, TRANSACTIONS.CATEGORY_ID), the same leftJoin(SPLITS)+join(ACCOUNTS), and the identical 4-predicate EXPENSE filter (USER_ID, TYPE=EXPENSE, DATE.between, TRANSFER_GROUP_ID.isNull). ADR-0015 warns every income/expense aggregate must carry the transfer-exclusion predicate, so missing it at one site is a silent correctness bug.
- _Fix:_ Add an internal ExpenseAggregation object next to RepositoryHelpers.kt exposing the effectiveAmount/effectiveCategoryId expressions plus a transferExcludedExpense(userId, from, to) Condition builder; call it from sumByCategory, fetchCategorySpendingOverTime and aggregateByCategory. Collapses the ADR-0015 invariant to one edit site.
- _Locations:_ centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/BudgetRepository.kt:87-102; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/ReportsRepository.kt:53-74; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/TransactionRepository.kt:383-398

**income/expense CASE-sum expressions and to_char month-key duplicated across aggregateByMonth/aggregateByDay and ReportsRepository**  
`duplication · sev:medium · conf:medium · effort:small`
- _Evidence:_ The incomeExpr/expenseExpr CASE-sum definitions are byte-for-byte identical between aggregateByMonth (420-427) and aggregateByDay (459-466); both share select/join(ACCOUNTS)/where(base + DATE.ge + TRANSFER_GROUP_ID.isNull)/groupBy shape, differing only in to_char format ('YYYY-MM' vs 'YYYY-MM-DD'), start-date and gap-filling. The month-key to_char(..,'YYYY-MM') exists as TXN_MONTH_KEY (ReportsRepository:22-26) and inline at TransactionRepository:419.
- _Fix:_ Extract the income/expense CASE-sum expressions and a parameterized periodKey(date, fmt) helper into the shared jooq aggregation helper, and collapse aggregateByMonth/aggregateByDay onto one private function parameterized by period format + start date (leave gap-filling per-caller).
- _Locations:_ centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/TransactionRepository.kt:420-427; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/TransactionRepository.kt:459-466; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/ReportsRepository.kt:22-26

**Transfer leg-pair insertion (OUT=EXPENSE / IN=INCOME with shared transfer_group_id + FX wiring) duplicated across two repositories**  
`duplication · sev:medium · conf:medium · effort:medium`
- _Evidence:_ Both build the ADR-0015 two-leg transfer: identical findManagedCategoryId(TRANSFER_OUT/IN) + 'Migration may not have run.' messages, one groupId=UUID.randomUUID(), destFx=FxColumns.from(conversion), a source leg (EXPENSE) and dest leg (INCOME, ORIGINAL_AMOUNT/CURRENCY/EXCHANGE_RATE/RATE_DATE) sharing groupId. insertTransfer uses .set()+returning and defers balance updates; materializeOnce uses .values(), sets RECURRING_TRANSACTION_ID and updates BALANCE inline.
- _Fix:_ Extract a private helper in RepositoryHelpers.kt that, given (sourceAccountId, destAccountId, sourceAmount, destAmount, FxColumns, description, date, now, recurringTransactionId: UUID? = null), resolves TRANSFER_OUT/IN ids and inserts both legs sharing one fresh transfer_group_id (returning the leg ids); both callers keep their own balance/fetch logic.
- _Locations:_ centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/TransactionRepository.kt:202-243; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/RecurringTransactionRepository.kt:220-257

### [HIGH] Cache-backed composables & CRUD store/service boilerplate

Four cache-backed aggregate composables are the same skeleton copy-pasted around the shared createAsyncCache, a runMutation helper is duplicated between two stores, and the per-action loading flag is named inconsistently (pending vs loading) across 14 stores. Stable surface, but every new aggregate or store re-types the same wrapper.

**Four cache-backed aggregate composables are the same skeleton copy-pasted; only the service call, type, key shape and log namespace differ**  
`duplication · sev:high · conf:high · effort:medium`
- _Evidence:_ Each has a module-level cache=createAsyncCache(()=>[]), a key(...) builder, a prefetchX wrapping cache.loadOrCache in try/catch logging via adze.ns, a useX composable with data/loading refs and a load() guarding !id->data=[], plus a watch([...],load,{immediate:true}) and invalidateX. Only the type, one-line service call, key arity and adze.ns string differ; the only real variance is use-account-snapshots' extra empty-args short-circuit.
- _Fix:_ Add a generic createCachedResource<TArgs, TData>({ key, fetch, ns }) factory returning { prefetch, use, invalidate }; use takes reactive arg getters and yields {data, loading, reload}. Each composable shrinks to wiring its service call, key fields, namespace and (for snapshots) its empty-args short-circuit.
- _Locations:_ centsible-ui/app/composables/use-category-aggregates.ts:1-81; centsible-ui/app/composables/use-account-snapshots.ts:1-79; centsible-ui/app/composables/use-daily-aggregates.ts:1-60; centsible-ui/app/composables/use-monthly-aggregates.ts:1-63

**runMutation helper copy-pasted between categoriesStore and rulesStore**  
`duplication · sev:low · conf:high · effort:small`
- _Evidence:_ Both files declare async function runMutation(action, successTitle, successBody, errorTitle) at lines 30-42 with byte-identical bodies (flip pending, await action(), await refetch, toasts.success, apiErrors.toastError + rethrow, reset pending in finally); only the refetch call and the generic-error i18n key differ.
- _Fix:_ Extract one shared composable runMutation(pending, apiErrors)(action, refetch, {successTitle, successBody, errorTitle, fallbackBody}) used by both. Note: an in-flight legacy categorization cleanup may remove rulesStore; if so, fold this into that cleanup rather than extracting independently.
- _Locations:_ centsible-ui/app/stores/categoriesStore.ts:30-42; centsible-ui/app/stores/rulesStore.ts:30-42

**Loading-flag name diverges across stores (pending vs loading)**  
`complexity · sev:low · conf:high · effort:small`
- _Evidence:_ 8 stores expose const pending = ref(false); 5 expose const loading = ref(false); reportsStore derives pending = computed(inflight>0). Components read both: store.loading at 3 sites (budgets-list:103, recurring-list:69, tags-manager:96) and store.pending at many. A developer must open each store to know which name is exposed.
- _Fix:_ Pick one name (majority is pending) and rename the 5 loading stores plus their 3 component reads in one tsc-checked rename so every setup-store exposes the same flag.
- _Locations:_ centsible-ui/app/stores/budgetAccountsStore.ts:15; centsible-ui/app/stores/contactsStore.ts:16; centsible-ui/app/stores/categoriesStore.ts:16; centsible-ui/app/stores/loansStore.ts:22; centsible-ui/app/stores/providersStore.ts:19; centsible-ui/app/stores/userStore.ts:12; centsible-ui/app/stores/systemInformationStore.ts:13; centsible-ui/app/stores/rulesStore.ts:16; centsible-ui/app/stores/budgetsStore.ts:10; centsible-ui/app/stores/tagsStore.ts:11; centsible-ui/app/stores/notificationsStore.ts:16; centsible-ui/app/stores/importTemplatesStore.ts:14; centsible-ui/app/stores/recurringTransactionsStore.ts:9; centsible-ui/app/stores/reportsStore.ts:22

### [MEDIUM] Repeated chart/formatting/preview UI helpers

Currency-string formatting, the month-year label, the foreign-currency conversion-preview hint, full Chart.js options blocks and budget-progress math are each re-implemented inline across multiple molecules/organisms instead of using (or extending) the existing atom/utils/composable, with confirmed visual drift between copies.

**Foreign-currency conversion-preview block copy-pasted across three forms**  
`duplication · sev:high · conf:high · effort:small`
- _Evidence:_ All three destructure useConversionPreview into {converted, failed, isForeign} and render the identical 6-line hint with three branches (BalanceNumberFormat / conversionUnavailable / fallback). Drift already present: transaction-form/transfer-form use text-neutral-400 while repayment-form uses text-muted and Number(form.amount)>0, proving the copies are diverging.
- _Fix:_ Extract a presentational <ConversionPreviewHint :converted :failed :foreign :amount :currency/> owning the v-if + three-branch markup; render it from all three forms.
- _Locations:_ centsible-ui/app/components/_molecules/transactions/transaction-form.vue:173-179; centsible-ui/app/components/_molecules/transactions/transfer-form.vue:74-80; centsible-ui/app/components/_molecules/loans/repayment-form.vue:88-94

**Currency-string formatting reimplemented inline in three molecules (atom only covers the display case)**  
`duplication · sev:medium · conf:high · effort:small`
- _Evidence:_ All three hand-roll new Intl.NumberFormat(localeTag.value,{style:'currency',currency}).format(amount) to get a currency STRING for an i18n arg; two add a toFixed(2) fallback for undefined currency. The atom balance-number-format.vue wraps the identical Intl call but renders into a <span>, and utils/money.ts has no formatCurrency, so options/locale live in 4 places.
- _Fix:_ Add formatCurrency(amount, currency?, locale) to utils/money.ts (keeping the toFixed(2) fallback); have balance-number-format.vue call it and replace the three inline NumberFormat usages.
- _Locations:_ centsible-ui/app/components/_molecules/loans/repayment-form.vue:46-50; centsible-ui/app/components/_molecules/transactions/set-balance-form.vue:44-49; centsible-ui/app/components/_molecules/budgets/budget-progress-bar.vue:34-37

**income-vs-expense chart re-implements the shared month-year label helper**  
`duplication · sev:low · conf:high · effort:small`
- _Evidence:_ Chart formatLabel splits 'yyyy-MM', builds new Date(year, month-1, 1) and calls toLocaleDateString — the same parse/format as formatMonthYearLabel in utils/date.ts:22-26 (used by budgets-list and budget-summary). Only the Intl options differ (short/2-digit vs the helper's hardcoded long/numeric).
- _Fix:_ Give formatMonthYearLabel an optional Intl options arg (default long/numeric) and call it from the chart with {month:'short',year:'2-digit'}, deleting the local formatLabel.
- _Locations:_ centsible-ui/app/components/_organisms/dashboard/income-vs-expense-chart.vue:27-32; centsible-ui/app/utils/date.ts:22-26

**Two reports charts duplicate the full Chart.js options block instead of a shared helper in use-chart-theme**  
`duplication · sev:medium · conf:high · effort:small`
- _Evidence:_ The entire chartOptions computed body (responsive/legend with tickColor+usePointStyle+font 11/tooltip currency callback/y-axis gridColor+currency ticks/x-axis grid off) differs by exactly one line between the two files (ChartOptions<'bar'> vs <'line'>). use-chart-theme.ts exposes tickColor/gridColor/currencyFmt but no options-shell factory.
- _Fix:_ Add a baseCurrencyChartOptions() factory to use-chart-theme.ts returning the shared legend/tooltip/scales object; each chart spreads it and overrides only datasets/type.
- _Locations:_ centsible-ui/app/components/_organisms/reports/cash-flow-chart.vue:37-58; centsible-ui/app/components/_organisms/reports/category-spending-chart.vue:41-62; centsible-ui/app/composables/use-chart-theme.ts

**Budget progress math (effectiveLimit / ratio / overBudget / barColor 0.85 threshold) duplicated across two budget molecules**  
`duplication · sev:medium · conf:high · effort:small`
- _Evidence:_ effectiveLimit = amountLimit + (rolloverAmount ?? 0), overBudget = ratio>1, and the 3-branch barColor (overBudget->bg-error; ratio>=0.85->bg-warning; else bg-success) are verbatim identical in both. Ratio scope differs (aggregate totals vs per budget) but the 0.85 threshold + barColor + per-budget effectiveLimit rule live in two files. models/budget exposes no helper.
- _Fix:_ Add effectiveLimit(b) and budgetBarColor(ratio) (0.85 threshold) to models/budget or a small composable; consume from both molecules.
- _Locations:_ centsible-ui/app/components/_molecules/budgets/budget-summary.vue:18-36; centsible-ui/app/components/_molecules/budgets/budget-progress-bar.vue:13-28

**Balance-change diff + up/down tone/icon logic duplicated in two presentational components**  
`duplication · sev:low · conf:medium · effort:small`
- _Evidence:_ Both compute the delta and map its sign to a trending icon + tone, each rendering BalanceNumberFormat for the delta. balance-change-badge derives {color,icon} (3-state); account-balance recomputes balanceChange + isUp (2-state) and inlines the same trending-up/down icon. Behavior differs slightly (3-state vs 2-state) and chip styling differs substantially, so reuse needs the badge to support the host's chip styling.
- _Fix:_ Have account-balance render balance-change-badge (passing current/previous) for the delta chip so the diff+icon+tone live in one component; low priority given the styling divergence.
- _Locations:_ centsible-ui/app/components/_molecules/badges/balance-change-badge.vue:11-26; centsible-ui/app/components/_molecules/dashboard/account-balance.vue:14-44

### [MEDIUM] Atomic-design tier leaks & passthrough/select boilerplate

Eight byte-identical app-* passthrough atoms, three near-identical searchable entity-select atoms (with category-select adding needless casting), an orphaned dead nav/profile organism, and three molecules that fetch/mutate stores in violation of ADR-0006 all point to thin-tier boilerplate and boundary erosion in the component layer.

**Eight byte-identical app-* passthrough wrappers in _atoms/ui**  
`duplication · sev:low · conf:medium · effort:medium`
- _Evidence:_ All 8 have an empty <script setup> and the identical slot-forwarding body, differing only in the wrapped U-tag; normalizing the tag yields one identical md5 across all 8. AppButton is referenced in 66 .vue files, so this is live boilerplate; any change to the slot-forwarding convention must be repeated in every wrapper.
- _Fix:_ Replace the duplicated template with a single shared slot-forwarding render helper / defineComponent factory (makePassthrough(UButton)), keeping one entry per component to preserve typed props/events. Weigh the tradeoff: a 10-line .vue is trivially readable and a factory can complicate per-component prop/event typing.
- _Locations:_ centsible-ui/app/components/_atoms/ui/app-button.vue:1-10; centsible-ui/app/components/_atoms/ui/app-checkbox.vue:1-10; centsible-ui/app/components/_atoms/ui/app-input.vue:1-10; centsible-ui/app/components/_atoms/ui/app-radio-group.vue:1-10; centsible-ui/app/components/_atoms/ui/app-select.vue:1-10; centsible-ui/app/components/_atoms/ui/app-select-menu.vue:1-10; centsible-ui/app/components/_atoms/ui/app-switch.vue:1-10; centsible-ui/app/components/_atoms/ui/app-textarea.vue:1-10

**account-/category-/contact-select share one UFormField+USelectMenu scaffold copied three times**  
`duplication · sev:medium · conf:high · effort:medium`
- _Evidence:_ All three have an identical props surface (name/label/description/hint/required/disabled/options), the same UFormField wrapper, and the same searchable USelectMenu (w-full, label-key='name', #default + #item-leading slots), differing only in model/options type, the leading visual and the placeholder key. grep for item-leading under _atoms returns exactly these three.
- _Fix:_ Extract a generic searchable entity-select atom owning the UFormField+USelectMenu scaffold, exposing #leading/#item-leading slots (or an iconKey/colorKey config) and a placeholder prop, so account/category/contact pass only their leading template and i18n key.
- _Locations:_ centsible-ui/app/components/_atoms/inputs/account-select.vue:1-45; centsible-ui/app/components/_atoms/inputs/contact-select.vue:1-46; centsible-ui/app/components/_atoms/inputs/category-select.vue:1-55

**category-select casts to SelectMenuItem and back through three abstractions its siblings don't need**  
`complexity · sev:low · conf:high · effort:small`
- _Evidence:_ category-select adds an items computed casting options 'as unknown as SelectMenuItem[]', a selected get/set round-tripping model through SelectMenuItem, and asCategory(item)=>item 'as unknown as Category' at every slot usage. Sibling account-select binds :items=options and v-model=model directly and reads fields straight off the slot, proving the same USelectMenu works without casting. The extra layer defeats type safety for no behavioral gain.
- _Fix:_ Drop items/selected/asCategory; bind :items=options and v-model=model and read fields directly like account-/contact-select. If a generic-typing issue forced the casts, fix it via the USelectMenu type parameter. Subsumed if the trio is unified.
- _Locations:_ centsible-ui/app/components/_atoms/inputs/category-select.vue:18-24; centsible-ui/app/components/_atoms/inputs/category-select.vue:42-52

**Two forms duplicate the category fetch the store already owns (and a third triggers a store mutation), breaking the molecule boundary (ADR-0006)**  
`complexity · sev:medium · conf:medium · effort:medium`
- _Evidence:_ budget-form and recurring-form both do useApi()+useCategoryService(api) and hand-roll category loading into a local ref with their own try/catch + adze (divergent: budgets filters EXPENSE namespace 'budgets'; recurring no filter namespace 'recurring'), a 3rd category-fetch path diverging from useCategoriesStore().updateCategories() (which has toasts + a pending flag). contact-avatar-uploader reads useContactsStore and triggers contactsStore.updateContactPicture from a molecule. A failed category load shows no toast in these two forms.
- _Fix:_ Have budget-form and recurring-form consume useCategoriesStore() (one category-fetch path) or lift the fetch to the parent organism and pass categories as a prop; lift the avatar mutation to an organism and emit the file. Leave read-only store consumers as a separate question.
- _Locations:_ centsible-ui/app/components/_molecules/budgets/budget-form.vue:19-31; centsible-ui/app/components/_molecules/recurring/recurring-form.vue:28-42; centsible-ui/app/components/_molecules/contacts/contact-avatar-uploader.vue:11-29

**Orphaned nav/profile.vue is dead code and duplicates the sidebar user-menu dropdown**  
`complexity · sev:medium · conf:high · effort:small`
- _Evidence:_ grep for NavProfile / nav/profile across app/ returns nothing; the only <Profile/> usage explicitly imports user/profile.vue, and this file would auto-import as <NavProfile> which is never used. Its dropdown items (profile/about/logout, same i18n keys/icons/navigateTo/authStore.logout()) duplicate the actively-used sidebar-user-menu.vue:13-21.
- _Fix:_ Delete nav/profile.vue. If a top-bar user dropdown is later reintroduced, factor the items array out of sidebar-user-menu.vue rather than reviving a divergent copy.
- _Locations:_ centsible-ui/app/components/_organisms/nav/profile.vue:1-53; centsible-ui/app/components/_organisms/nav/sidebar-user-menu.vue:13-21

**totp-section repeats the same busy-guard + invalid-code handler skeleton three times**  
`complexity · sev:low · conf:low · effort:small`
- _Evidence:_ onConfirm, onDisable and onRegenerate each repeat if(busy)return; busy=true; try{ await totpService.X(state.code.trim()); ... }catch{ error(invalidCodeTitle/Body) }finally{ busy=false }, differing only in the service call and success transition. The three UForm blocks also share codeSchema+BaseInput name='code'.
- _Fix:_ Extract a small runWithCode(serviceCall, onSuccess) helper wrapping the busy-guard + invalid-code toast; the form-molecule extraction is optional/lower priority.
- _Locations:_ centsible-ui/app/components/_organisms/user/totp-section.vue:67-81; centsible-ui/app/components/_organisms/user/totp-section.vue:96-109; centsible-ui/app/components/_organisms/user/totp-section.vue:121-135

### [MEDIUM] Backend service-layer duplication (transaction pipeline, ownership guard, crypto, parsing)

Within centsible-core, the create/update transaction pipeline, the category-ownership guard (with one site leaking a non-localized exception, violating ADR-0004), and the AES-GCM cipher kernel are each duplicated, and the cross-stack rule operator-mapping is encoded three times across two modules and two languages.

**createTransaction and updateTransaction duplicate the convert/resolve/normalize/persist pipeline**  
`duplication · sev:medium · conf:high · effort:medium`
- _Evidence:_ Both fetch the account, call currencyConversionService.convert(amount, currency ?: account.currency, account.currency, date), resolveType(...), normalizeSplits(...), build resolvedForm = form.copy(type=resolvedType, categoryId=splits?.first()?.categoryId ?: form.categoryId), then re-fetch splits, updateBalance, checkBudgetAlerts, checkInlineTransactionAlerts. update() differs only by old-state bookkeeping.
- _Fix:_ Extract the shared prologue into a private resolveForPersist(account, form, user) (conversion + resolvedType + splits + resolvedForm) and a shared persistSplitsAndAlert(...) epilogue; update() retains only its old-adjustment / old-split-category diffing.
- _Locations:_ centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/transactions/TransactionService.kt:105-127; centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/transactions/TransactionService.kt:148-176

**Category-ownership guard reimplemented per service with divergent error types**  
`duplication · sev:low · conf:high · effort:small`
- _Evidence:_ The same lookup (categoriesRepository.fetchCategoryClassifications(user, listOf(id)) then assert id present) is reimplemented four times. BudgetService and TransactionService both throw LocalizedException.BadRequest('error.category.notAccessible'); RuleService repeats it; RecurringTransactionService.assertCategoryOwned throws a raw IllegalArgumentException for the identical condition — a non-localized leak violating ADR-0004.
- _Fix:_ Add one shared guard (e.g. internal ICategoriesRepository.requireOwned(user, categoryId) or a core helper) that does the lookup and throws LocalizedException.BadRequest('error.category.notAccessible') uniformly; route all four call sites through it, fixing the Recurring IllegalArgumentException leak.
- _Locations:_ centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/budget/BudgetService.kt:63-70; centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/recurring/RecurringTransactionService.kt:154-160; centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/transactions/TransactionService.kt:588-599; centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/rule/RuleService.kt:116-117

**Rule per-field valid-operators mapping encoded three times (RuleService validation, RuleMatching when-arms, frontend rule.ts) kept in sync only by a comment**  
`duplication · sev:medium · conf:high · effort:small`
- _Evidence:_ The legal-operator-per-field mapping (DESCRIPTION->CONTAINS/EQUALS/STARTS_WITH, AMOUNT->GT/GTE/LT/LTE/EQUALS, DIRECTION/ACCOUNT->IS) is stated three times: rule.ts:13-18 (comment 'mirrors the backend RuleService validation'), RuleService.kt:94-98 against companion sets DESCRIPTION_OPERATORS/AMOUNT_OPERATORS (130-132), and RuleMatching.kt:33-59 as when-arms with else->false. core depends on api, so RuleService can consume a single source declared in RuleMatching.
- _Fix:_ Expose one source of truth on the backend (a RuleField.validOperators map in RuleMatching) and have RuleService.validateCondition consult it instead of the private operator sets, collapsing the two backend copies to one; the FE rule.ts copy remains an unavoidable cross-language mirror.
- _Locations:_ centsible-ui/app/models/rule/rule.ts:13-18; centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/rule/RuleService.kt:94-98; centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/rule/RuleService.kt:130-132; centsible-api/src/main/kotlin/beer/thierry/centsible/api/model/rule/RuleMatching.kt:33-59

**TotpSecretCipher and CredentialCipher duplicate the AES-GCM encryptor construction and salt validation**  
`duplication · sev:low · conf:high · effort:small`
- _Evidence:_ Both contain the identical lazy encryptor kernel (require key not blank, require salt matches HEX_SALT_REGEX, Encryptors.stronger(key, salt)), the same HEX_SALT_REGEX constant, and the same blank-key boot warning; only the env-var names and the payload codec (UTF-8 string vs JSON map) differ. A crypto-policy change must be edited in two places.
- _Fix:_ Extract the shared kernel into a small base (abstract EnvBackedBytesEncryptor(keyProperty, saltProperty) exposing the validated lazy BytesEncryptor + salt regex); TotpSecretCipher (string) and CredentialCipher (JSON map) supply only their codec, keeping the two distinct key/salt sources but defining construction/validation once.
- _Locations:_ centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/authentication/TotpSecretCipher.kt:26-48; centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/integrations/CredentialCipher.kt:35-93

### [MEDIUM] Plugin-SPI shared logic duplicated across import/integration submodules

Logic that ADR-0005 expects to live in the per-SPI shared-helper modules (imports/core, integrations/support) is instead copy-pasted across format parsers, provider configs, importers, the REST probe and the export renderers: row-mapping tails, HTTP-timeout wiring, date fallbacks, per-export-type request unpacking, and inline core->DTO mapping.

**Shared row-mapping tail, amount/CategoryType convention, warning-summary logging, default-category precondition and parse-error wrapper duplicated across CsvFileParser and OfxFileParser**  
`duplication · sev:medium · conf:high · effort:small`
- _Evidence:_ logWarningSummary is byte-identical bar the 'CSV'/'OFX' literal; the ImportTransactionRow tail (amount.abs() + sign->EXPENSE/INCOME) is identical; the defaultCategoryId requireNotNull precondition is identical bar the class name; the outer try/catch(log.error+rethrow) is identical; the skip-zero/missing-amount rule recurs in two idioms. ADR-0005 carves out shared-helper modules, so this translation/logging logic is not covered by the per-format-split exemption.
- _Fix:_ Add top-level helpers in centsible-imports/core (avoid an abstract base): logWarningSummary(format, warnings, log); toImportRow(amount, description, date, defaultCategoryId) applying abs()+sign->CategoryType once; requireDefaultCategoryId(hints); optionally a runParse(format, bytesSize, log){} wrapper. Both parsers call these; new formats inherit the tail for free.
- _Locations:_ centsible-imports/csv/src/main/kotlin/beer/thierry/centsible/imports/csv/CsvFileParser.kt:69-95; centsible-imports/csv/src/main/kotlin/beer/thierry/centsible/imports/csv/CsvFileParser.kt:103-108; centsible-imports/csv/src/main/kotlin/beer/thierry/centsible/imports/csv/CsvFileParser.kt:152-165; centsible-imports/ofx/src/main/kotlin/beer/thierry/centsible/imports/ofx/OfxFileParser.kt:50-104; centsible-imports/ofx/src/main/kotlin/beer/thierry/centsible/imports/ofx/OfxFileParser.kt:111-116; centsible-imports/ofx/src/main/kotlin/beer/thierry/centsible/imports/ofx/OfxFileParser.kt:124-146

**HTTP-timeout RestClient wiring (property keys + 10000/30000 defaults + builder chain) duplicated across all three provider configs**  
`duplication · sev:medium · conf:high · effort:small`
- _Evidence:_ Each repeats the identical @Value pair with hardcoded defaults and keys (integrations.http.connect-timeout-ms:10000 / read-timeout-ms:30000) and the identical RestClient.builder().requestFactory(timeoutRequestFactory(...)).build() chain. support/HttpTimeouts.kt owns timeoutRequestFactory but NOT the default literals or keys, so changing the default read timeout requires editing three files; baseUrl/secrets stay local.
- _Fix:_ Add to the support module a @ConfigurationProperties('integrations.http') bean (or a RestClient.Builder.withIntegrationTimeouts() extension) holding the connect/read defaults in one place; each config injects it and calls one helper, keeping per-provider baseUrl/secrets wiring local.
- _Locations:_ centsible-integrations/paypal/src/main/kotlin/beer/thierry/centsible/integrations/paypal/PaypalIntegrationConfig.kt:16-32; centsible-integrations/banking-gocardless/src/main/kotlin/beer/thierry/centsible/integrations/banking/gocardless/GoCardlessIntegrationConfig.kt:20-44; centsible-integrations/fx-frankfurter/src/main/kotlin/beer/thierry/centsible/integrations/fx/frankfurter/FrankfurterFxConfig.kt:18-33

**Per-export-type request unpacking duplicated across the 3 format renderers (PDF/CSV/JSON)**  
`duplication · sev:medium · conf:high · effort:medium`
- _Evidence:_ All 12 blocks (4 types x 3 formats) verified. The Transactions parse block (require+userId+accountIds+fromDate+toDate+categoryIds) is byte-identical across PDF/Csv/Json; the LendingsPerContact block (require+userId+contactId+fetchContactSummary ?: error('Contact $contactId not found for user $userId')+fetchLoansForContact) is identical across the three — the triplicated not-found error path is the real drift risk. Format-specific framing is already deduped into CsvExportRenderer/JsonExportRenderer base classes.
- _Fix:_ Add per-type parse helpers in the existing RenderingHelpers.kt (alongside baseMeta/locale/slug): ExportRequest.transactionsParams()/lendingsPerContactCtx() (the latter doing the summary/loans fetch + not-found error once) plus a shared ExportRequest.userId(); each renderer keeps only its format-specific output mapping.
- _Locations:_ centsible-export/src/main/kotlin/beer/thierry/centsibleexport/render/impl/TransactionsRenderer.kt:26-32; centsible-export/src/main/kotlin/beer/thierry/centsibleexport/render/impl/TransactionsCsvRenderer.kt:17-23; centsible-export/src/main/kotlin/beer/thierry/centsibleexport/render/impl/TransactionsJsonRenderer.kt:19-25; centsible-export/src/main/kotlin/beer/thierry/centsibleexport/render/impl/LendingsPerContactRenderer.kt:24-30; centsible-export/src/main/kotlin/beer/thierry/centsibleexport/render/impl/LendingsPerContactCsvRenderer.kt:17-23; centsible-export/src/main/kotlin/beer/thierry/centsibleexport/render/impl/LendingsPerContactJsonRenderer.kt:19-25; centsible-export/src/main/kotlin/beer/thierry/centsibleexport/render/impl/LendingsAllRenderer.kt:22-24; centsible-export/src/main/kotlin/beer/thierry/centsibleexport/render/impl/AccountsSummaryRenderer.kt:23-25

**csvProbe hand-maps core CsvColumnMapping/CsvDialect to their DTOs inline, duplicating the Konvert mapper's responsibility**  
`complexity · sev:low · conf:high · effort:small`
- _Evidence:_ csvProbe manually copies core CsvColumnMapping->CsvColumnMappingDTO field-by-field (12 fields, 70-83) and CsvDialect->CsvDialectDTO (4 fields, 89-94). The @Konverter ImportMappers already declares the toCore direction for both exact type-pairs but no reverse; the two DTOs have NO core-only fields so a reverse mapping is total. A forgotten field in the controller block compiles fine while silently mapping to the default — mapper logic deviating into the REST layer against the onion convention.
- _Fix:_ Add toDto(core: CsvColumnMapping): CsvColumnMappingDTO and toDto(core: CsvDialect): CsvDialectDTO to the existing ImportMappers @Konverter interface, then in csvProbe replace the inline copies with the generated mappers; keeps both directions in one compiler-checked place.
- _Locations:_ centsible-rest/src/main/kotlin/beer/thierry/centsiblerest/resources/ImportsResource.kt:69-94; centsible-core/src/main/kotlin/beer/thierry/centsible/core/services/imports/ImportMappers.kt:18-23; centsible-api/src/main/kotlin/beer/thierry/centsible/api/services/imports/ParseHintsDTO.kt:27-47

**Lendings-per-contact filename slug derived from two different sources (name vs UUID) across formats**  
`duplication · sev:low · conf:high · effort:small`
- _Evidence:_ PDF renderer uses filenameStem='lendings-${slug(summary.contactName)}' while Csv and Json both use 'lendings-${slug(request.lendingsPerContact.contactId)}' (UUID). The same export yields inconsistent, partly-unfriendly filenames across formats; the CSV/JSON variants fall back to the UUID because they have no contact name at filenameStem() time without re-fetching — which couples this to the per-type parse-holder extraction.
- _Fix:_ Standardize on the contact name: have all three derive filenameStem from the shared parsed holder's contactName (introduced via the per-export-type parse helper), so every format produces lendings-<name>.
- _Locations:_ centsible-export/src/main/kotlin/beer/thierry/centsibleexport/render/impl/LendingsPerContactRenderer.kt:49; centsible-export/src/main/kotlin/beer/thierry/centsibleexport/render/impl/LendingsPerContactCsvRenderer.kt:52-53; centsible-export/src/main/kotlin/beer/thierry/centsibleexport/render/impl/LendingsPerContactJsonRenderer.kt:52-53

**Date-only-to-UTC OffsetDateTime fallback branch duplicated in both transaction importers**  
`duplication · sev:low · conf:low · effort:small`
- _Evidence:_ Both, after a primary OffsetDateTime.parse attempt, fall back to LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime() in try/catch(DateTimeParseException)->null; the two fallback expressions are behaviorally equivalent. Only the fallback half matches; the primary attempt and surrounding intent differ (PayPal mixed-offset formatter vs GoCardless bookingDateTime/Date/valueDate precedence).
- _Fix:_ Add fun parseDateOnlyAtUtc(value: String): OffsetDateTime? = runCatching { LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime() }.getOrNull() to the support module and call it from both fallback branches. Genuinely shared but a small ~3-line win.
- _Locations:_ centsible-integrations/paypal/src/main/kotlin/beer/thierry/centsible/integrations/paypal/PaypalProviderModule.kt:266-270; centsible-integrations/banking-gocardless/src/main/kotlin/beer/thierry/centsible/integrations/banking/gocardless/GoCardlessProviderModule.kt:337-341

### [LOW] Repository read-projection boilerplate

A couple of repositories inline a multi-column SELECT projection twice (per method) or hand-define DSL.field constants that shadow the committed jOOQ codegen, both bypassing patterns the codebase already uses elsewhere (categoryProjection, RECORD_COLUMNS, generated table references).

**AttachmentRepository hand-defines DSL.field/table constants that duplicate the generated table references**  
`complexity · sev:low · conf:high · effort:small`
- _Evidence:_ L16-24 declare ATTACHMENTS/A_ID/A_TXN/A_USER/A_FILENAME/A_CONTENT_TYPE/A_SIZE/A_STORAGE_KEY/A_CREATED_AT via DSL.table/DSL.field, duplicating the generated TransactionAttachments (ID, TRANSACTION_ID, USER_ID, FILENAME, CONTENT_TYPE, SIZE_BYTES, STORAGE_KEY, CREATED_AT). listForUser (L70-100) already uses the generated TRANSACTION_ATTACHMENTS reference, so one file mixes two styles and the string-keyed constants bypass the rename/type safety jOOQ codegen provides (ADR-0013).
- _Fix:_ Delete the manual constants (L16-24) and rewrite create/listForTransaction/countByTransactionIds/fetch/delete to use TRANSACTION_ATTACHMENTS.* (note SIZE_BYTES vs A_SIZE) as listForUser already does.
- _Locations:_ centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/AttachmentRepository.kt:16-24; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/AttachmentRepository.kt:40-49; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/AttachmentRepository.kt:70-100

**Large SELECT projections inlined twice per repo instead of an extracted field list (Contacts, Budget)**  
`duplication · sev:low · conf:high · effort:small`
- _Evidence:_ ContactsRepository.fetchAllContacts and fetchContactById repeat an identical 11-column CONTACTS+CONTACT_BALANCES projection and the same leftJoin verbatim, differing only in WHERE. BudgetRepository.fetchAllWithSpentForMonth selects 12 cols incl. BUDGETS.CATEGORY_ID; fetchById repeats the same list minus that one column. The sibling pattern already exists (CategoriesRepository.categoryProjection, ProviderConnectionsRepository.RECORD_COLUMNS, ExportJobRepository.JOB_COLUMNS, RecurringTransactionRepository.baseSelect()).
- _Fix:_ Extract each repo's column list to a private val xProjection: Array<Field<*>> (or a baseSelect() builder) reused by both methods; for Budget use one projection that includes BUDGETS.CATEGORY_ID in both (harmless extra column in fetchById's mapToDTO).
- _Locations:_ centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/ContactsRepository.kt:20-37; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/ContactsRepository.kt:41-57; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/BudgetRepository.kt:28-41; centsible-jooq/src/main/kotlin/beer/thierry/centsible/jooq/repository/BudgetRepository.kt:108-120
