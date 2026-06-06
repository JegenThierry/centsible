# Changelog

All notable changes to Centsible are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.4.0] - 2026-06-06

### Added

- **Multi-currency support** with exchange-rate integration: transactions and
  recurring transactions now track an original amount/currency, exchange rate,
  and rate date, backed by a new `exchange_rates` table and a quote provider.
- **Account integrations**: GoCardless (open banking) and PayPal provider
  modules, plus the `provider_connections` schema and provider-connection
  account claiming.
- **Categorization rules**: user-defined rules (`CONTAINS`/`EQUALS`/`STARTS_WITH`)
  for auto-assigning categories, with management UI, and a managed
  "Uncategorized" system category.
- **Dashboard**: activity heatmap and daily aggregates.
- **Account management** UI and global navigation enhancements.
- **Continuous integration**: Woodpecker CI pipeline and test coverage for key
  modules.
- Architecture Decision Records (`docs/adr/`) documenting backend architecture,
  frontend design, and development conventions.
- `INTEGRATIONS_MANUAL_ENABLED` runtime configuration option.

### Changed

- Standardized HTTP timeouts and improved resilience across core and
  integration modules.
- Introduced Spring Data Web support and refactored UI select components,
  modals, caching, and atomic UI wrappers.
- Upgraded Nuxt and other dependencies; added Guava with enforced version
  constraints.
- Added a `transactions` index on `(account_id, transaction_date, id)` to
  optimize queries.

### Fixed

- `deleteTransaction` now validates account ownership and enforces constraints.
- 401 interceptor clears user state and redirects on session expiration.

### Security

- Neutralized CSV formula-injection risks in exported data.
- Integrated OWASP Dependency-Check into the build and upgraded dependencies to
  pull CVE fixes.
- Enforced category ownership validation; added tests ensuring the global
  exception handler does not leak sensitive information.

## [0.3.0] - 2026-05-22

Earlier releases predate this changelog. See the Git history and the
[`v0.3.0`](https://codeberg.org/thierryjegen/centsible/releases/tag/v0.3.0) tag
(and earlier `v0.1.0`–`v0.2.0` tags) for details.

[0.4.0]: https://codeberg.org/thierryjegen/centsible/compare/v0.3.0...v0.4.0
[0.3.0]: https://codeberg.org/thierryjegen/centsible/releases/tag/v0.3.0
