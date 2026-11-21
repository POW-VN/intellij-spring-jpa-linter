# Spring Data JPA Query Method Linter (IntelliJ Plugin)

Real-time validation, syntax typo detection, and instant SQL/JPQL preview for **Spring Data JPA** derived query methods in IntelliJ IDEA (both Community and Ultimate editions).

---

## 🌟 Key Features

* **Real-Time Typo Detection:** Automatically matches query method names against JPA `@Entity` fields.
* **Smart Suggestions:** Catches typos in query verbs (`findsTop` -> `findTop`), operators (`Betwen` -> `Between`), and sort directions (`Desssc` -> `Desc`).
* **Instant SQL / JPQL Preview:** Inspect the generated JPQL and SQL query directly on hover or `Ctrl + Q`.
* **Deep Multi-Level Entity Traversal:** Supports complex nested relationships (`findByRoom_Building_Manager_Name`).
* **100% Offline & Private:** Zero external cloud requests, ultra-fast in-memory processing.

---

## 🛠️ Testing & Verification

The plugin includes over 200 automated unit test cases:

```bash
mvn test
```

---

## 📦 Packaging

To build the plugin distribution ZIP package:

```bash
mvn package
```

The plugin archive is packaged at `spring-jpa-linter-0.2.3.zip`, ready for deployment to the [JetBrains Marketplace](https://plugins.jetbrains.com).

---

## 📄 License

Licensed under the [MIT License](LICENSE).  
Copyright (c) 2026 POW-VN.
