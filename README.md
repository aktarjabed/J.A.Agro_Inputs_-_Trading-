# INBusiness Architecture Hardening (v1.0.0-production-bundle-final)

## Core Architectural Invariants

* **BusinessContext & Repository Boundary:** All DAOs queries (read, update, delete) are business-scoped at the SQL layer. The application uses `BusinessContext` internally inside `InvoiceRepository`. The UI / ViewModels do not inject arbitrary business IDs.
* **Idempotency & Request Fingerprint:** Every invoice creation uses an `idempotencyKey` coupled with a SHA-256 `requestFingerprint`. The fingerprint canonicalizes the seller snapshot, customer data, and all financial totals and items. Repeated network calls with the same key safely replay the invoice response, but modified payloads result in a safe rejection.
* **Single Financial Truth:** All invoice UI logic derives totals strictly from `CalculateInvoiceTotalsUseCase`, eliminating independent tax, subtotal, and math aggregations scattered around `InvoiceViewModel`. The logic properly rounds to two decimal places at the line level.
* **PDF Snapshot Isolation:** The PDF generator strictly consumes historical, immutable snapshots (`sellerName`, `sellerGSTIN`, `subtotal`, `taxAmount`) directly from the Room database. It does not perform internal independent mathematical calculations, nor does it reach back out to mutable `BusinessData`.

## Verified Tests
- `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` run smoothly without dependencies or build regressions. Note: UI Instrumentation testing execution is deferred to the cloud/emulators in GitHub CI as per guidelines.
