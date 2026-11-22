# Changelog

All notable changes to the **Spring Data JPA Query Method Linter (IntelliJ Plugin)** will be documented in this file.

## [0.3.2] - 2026-11-22

### Added
- **Subject Descriptor Support:** Allows entity names in prefixes such as `findDistinctInvoiceByStatus` and `findTop3InvoicesBy...`.
- **`AllIgnoreCase` Suffix Support:** Properly parses `AllIgnoreCase` at the end of method names without false property errors.
- **Dynamic Projections Support:** Recognizes dynamic projection parameter `Class<T> type` and ignores it during parameter count validation.
- **PascalCase Property Validation:** Detects lowercase property names following `By`, `And`, `Or` (e.g. `findBycreatedat`) and suggests `findByCreatedAt`.
- **Conjunction Anomalies Check:** Flags trailing (`findByStatusAnd`), leading (`findByAndStatus`), and consecutive (`findByStatusAndOrRole`) conjunctions.
- **Signature & Return Type Validation:**
  - Flags methods returning `Page<T>` or `Slice<T>` that lack a `Pageable` parameter.
  - Ensures `countBy` returns a numeric type (`long`, `Long`, `int`, `Integer`).
  - Ensures `existsBy` returns `boolean` or `Boolean`.

---

## [0.3.1] - 2026-11-21

### Added
- Support for `All` prefix modifier: `findAllByOrderByCreatedAtDesc`, `findAllDistinctByStatus`, `findDistinctAllByStatus`.
- Support for collection emptiness operators: `IsEmpty` and `IsNotEmpty`.
- Excluded Spring Data 3.1+ `Limit` and `ScrollPosition` parameters from criteria count checks.
- Strict modifier typo detection: detects typos of `All` (`Aaallll`, `Alll`, `Al`), `Distinct` (`Distnct`), limit count 0 (`Top0`), and conjunction typos (`Andd`, `Orr`).

---

## [0.3.0] - 2026-11-21

### Added
- **1-Click Quick Fix (`Alt + Enter`):** Press `Alt + Enter` on flagged errors to rename methods to suggested correct names.
- Quick Fix to add `@Query("")` annotation directly on methods with syntax or operator errors.

---

## [0.2.3] - 2026-11-21

### Added
- Real-time derived query method validation in IntelliJ IDEA (Community & Ultimate).
- Instant JPQL and SQL preview on hover tooltips and `Ctrl + Q`.
- Deep multi-level entity relationship navigation (`_` and CamelCase).
