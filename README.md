# Spring Data JPA Query Method Linter (IntelliJ IDEA Plugin)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Tests: 240 Passing](https://img.shields.io/badge/Tests-240%20Passing-brightgreen.svg)](https://github.com/POW-VN/intellij-spring-jpa-linter)
[![Version: 0.4.0](https://img.shields.io/badge/Version-0.4.0-blue.svg)](https://github.com/POW-VN/intellij-spring-jpa-linter)
[![Platform: 2023.1+](https://img.shields.io/badge/IntelliJ-2023.1+-orange.svg)](https://plugins.jetbrains.com)

Real-time validation, syntax typo detection, 1-click Quick Fix, and instant SQL/JPQL preview for **Spring Data JPA** derived query methods in IntelliJ IDEA (compatible with both **Community** and **Ultimate** editions).

Catch errors in repository method names **as you type**, without waiting for Spring Boot application startup or running slow tests!

---

## 🚀 Quick Start (Hướng Dẫn Nhanh)

1. **Open any Spring Data Repository interface:**
   ```java
   @Repository
   public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
       // Type your query method here!
   }
   ```

2. **Real-Time Typo Detection:**
   When an invalid property, verb, operator, or conjunction is typed, IntelliJ immediately flags it with an inspection warning or error.

3. **1-Click Quick Fix (`Alt + Enter`):**
   * Place the cursor on the flagged method name.
   * Press **`Alt + Enter`** (Show Context Actions).
   * Choose **Rename method to '...'** to auto-fix the typo, or **Add '@Query("")' annotation**.

4. **Instant SQL / JPQL Preview (`Ctrl + Q`):**
   * Hover over the method or press **`Ctrl + Q`** (Quick Documentation / macOS: `F1`) to see the generated SQL, JPQL, criteria, and sort breakdown.

---

## 🌟 Key Features

* **Real-Time Typo Detection:** Automatically matches query method names against JPA `@Entity` fields.
* **Smart "Did you mean?" Quick Fixes (`Alt + Enter`):** Catches typos in verbs, operators, sort directions, and properties.
* **Prefix & Modifier Support:** Supports `All`, `Distinct`, `Top<N>`, `First<N>`, and entity subject descriptors (e.g. `findDistinctInvoiceByStatus`, `findTop3InvoicesBy...`).
* **Instant SQL / JPQL Preview:** Inspect the generated JPQL and SQL query directly on hover or `Ctrl + Q`.
* **Deep Multi-Level Entity Traversal:** Supports complex nested relationships (`findByRoom_Building_Manager_Name` or CamelCase).
* **Signature & Return Type Validation:** Checks for required `Pageable` parameter on `Page<T>`, valid numeric return types on `countBy`, and boolean on `existsBy`.
* **100% Offline & Private:** Zero external cloud requests, ultra-fast in-memory processing.

---

## 📥 Installation

### Method 1: Install from JetBrains Marketplace
1. In IntelliJ IDEA, open **Settings / Preferences** (`Ctrl + Alt + S`).
2. Navigate to **Plugins** ➔ **Marketplace** tab.
3. Search for **`Spring Data JPA Query Method Linter`** and click **Install**.
4. Restart IntelliJ IDEA if prompted.

### Method 2: Install from ZIP (Offline / Manual)
1. Download the latest `spring-jpa-linter-0.3.3.zip`.
2. In IntelliJ IDEA, go to **Settings / Preferences** (`Ctrl + Alt + S`) ➔ **Plugins**.
3. Click the gear icon ⚙️ ➔ select **`Install Plugin from Disk...`**.
4. Choose `spring-jpa-linter-0.3.3.zip` and click **OK**.
5. Restart the IDE.

---

## 📖 Supported Spring Data JPA Keywords

| Category | Supported Keywords | Example |
| :--- | :--- | :--- |
| **Query Verbs** | `find...By`, `read...By`, `get...By`, `query...By`, `search...By`, `stream...By` | `findByStatus(String s)` |
| **Aggregates & Checks** | `count...By`, `exists...By`, `delete...By`, `remove...By` | `countByStatus(String s)` |
| **Limiting Modifiers** | `Distinct`, `Top<N>`, `First<N>`, `All` | `findTop5ByOrderByCreatedAtDesc()` |
| **Logical Operators** | `And`, `Or` | `findByStatusAndRole(String s, String r)` |
| **Comparison** | `Between`, `LessThan`, `LessThanEqual`, `GreaterThan`, `GreaterThanEqual`, `After`, `Before` | `findByCreatedAtBetween(start, end)` |
| **Equality & Nullability** | `IsNull`, `IsNotNull`, `NotNull`, `True`, `False` | `findByIsActiveTrue()` |
| **String Matching** | `Like`, `NotLike`, `StartingWith`, `EndingWith`, `Containing`, `IgnoreCase`, `AllIgnoreCase` | `findByLastNameIgnoreCase(String n)` |
| **Collection Operators** | `In`, `NotIn`, `IsEmpty`, `IsNotEmpty` | `findByStatusIn(List<String> list)` |
| **Ordering** | `OrderBy<Property>[Asc\|Desc]` | `findAllByOrderByCreatedAtDesc()` |
| **Special Parameters** | `Pageable`, `Sort`, `Limit`, `ScrollPosition`, `Class<T>` | `findByStatus(String s, Pageable p)` |

---

## 🛠️ Testing & Verification

The plugin includes over **235 automated unit test cases**:

```bash
mvn test
```

---

## 📄 License

Licensed under the [MIT License](LICENSE).  
Copyright (c) 2026 POW-VN.
