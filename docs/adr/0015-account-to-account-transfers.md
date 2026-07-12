# ADR-0015: Account-to-Account Transfers as Linked Transaction Legs

Status:  Accepted
Date:    2026-06-06
Scope:   backend · frontend · database

## Context

Moving money between two of a user's own accounts is not income or spending — but the data model
only has single-account `transactions` typed `INCOME`/`EXPENSE`, and balances are derived from those
rows. A transfer must (a) move money out of one account and into another, (b) keep both balances
correct, (c) **not** distort income/expense budgets and reports, and (d) behave as one thing when
edited or deleted. Transfers exist for both one-off transactions and recurring rules.

## Decision

- A transfer is **two linked transaction rows** sharing a generated `transactions.transfer_group_id`
  (UUID): an `EXPENSE` leg on the source account and an `INCOME` leg on the destination. The legs are
  tagged with the system-managed categories **`Transfer out`** / **`Transfer in`** (seeded in
  `tables.sql`, resolved by name via `ManagedCategoryNames` + `findManagedCategoryId`, same pattern as
  Lending/Repayment — ADR-0003 managed categories).
- The amount is entered in the **source account's currency**; the destination leg stores the
  FX-converted amount plus the original amount/currency/rate (`FxColumns`), exactly like any
  multi-currency transaction.
- **`transfer_group_id` — not the category — is the exclusion marker.** Every income/expense SUM
  (`TransactionRepository.aggregateBy*`, `ReportsRepository`, `BudgetRepository.sumByCategory`) adds
  `AND transfer_group_id IS NULL`. Account **balances** and the `account_history` view deliberately
  **include** transfer legs (each leg genuinely moves its own account). Keying off the group id (not
  `is_managed`) leaves Lending/Repayment — also managed — counting as real income/expense.
- Transfers are **managed as a unit** in the service layer: deleting any leg deletes the whole group
  and reverses every leg's balance; editing replaces the group (reverse + delete + re-create).
- **Both accounts' ownership is checked in the service layer** (`fetchAccountById` for each), and
  source ≠ destination is enforced — never trust the destination id from the request (ADR-0003).
- Recurring transfers: `recurring_transactions.is_transfer` + `destination_account_id`; materialization
  emits the two legs and moves both balances. Single-account recurring rules may carry an explicit
  direction override in `recurring_transactions.type` (`NULL` = derive from the category).

## Examples

**Do**
- Identify / hide / exclude a transfer by `transfer_group_id`, e.g. `TransactionDTO.transferGroupId`
  drives the edit/delete-as-a-unit branch in `TransactionService` and the transfer badge in the UI.
- Resolve `Transfer out` / `Transfer in` by their `ManagedCategoryNames` constants at insert time.
- Convert the entered (source-currency) amount to the destination currency at the occurrence/date.

**Don't**
- Exclude transfers from reports by filtering on `is_managed` — that would also drop Lending/Repayment.
- Exclude transfer legs from account balances or `account_history` — the money really moved.
- Give a transfer a user category, or edit/delete only one leg of a posted transfer.

## Consequences

- One additive column (`transactions.transfer_group_id`) makes transfers linkable, identifiable, and
  report-excludable without a separate table or a new `type` enum value, so the existing
  INCOME/EXPENSE balance math is untouched.
- Reports/budgets stay clean while balances and net worth stay correct.
- Cost: every new income/expense aggregate must remember the `transfer_group_id IS NULL` predicate,
  and transfer create/edit/delete must operate on the whole group (two balance updates, not one).
- Edge: deleting an account cascade-deletes its legs but leaves the counterpart legs on the surviving
  account (they reflect real historical balance changes); the surviving orphan is still safely
  deletable as a (now single-leg) group.
